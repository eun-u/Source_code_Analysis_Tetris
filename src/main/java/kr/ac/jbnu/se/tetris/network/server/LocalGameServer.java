package kr.ac.jbnu.se.tetris.network.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.ac.jbnu.se.tetris.battle.BattleManager;
import kr.ac.jbnu.se.tetris.battle.BattleResult;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantSpec;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;
import kr.ac.jbnu.se.tetris.network.RequestOutcome;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.network.protocol.WireCodec;
import kr.ac.jbnu.se.tetris.network.protocol.WireRequest;

/** 로컬 TCP 방과 권위 있는 전투 상태를 소유하는 개발용 서버 */
public final class LocalGameServer implements AutoCloseable {
    private static final int DEFAULT_PORT = 28080;
    private static final int MAX_CONNECTIONS = 16;
    private static final int MAX_PENDING_REQUESTS = 64;
    private static final int MAX_PENDING_UPDATES = 128;
    private static final long TICK_MILLIS = 400;

    private final int requestedPort;
    private final ScheduledThreadPoolExecutor roomExecutor;
    private final Map<String, Room> rooms = new LinkedHashMap<String, Room>();
    private final ConcurrentHashMap<Peer, Boolean> peers = new ConcurrentHashMap<Peer, Boolean>();
    private volatile ServerSocket listener;
    private volatile Thread acceptThread;
    private volatile boolean closed;

    public LocalGameServer(int port) {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Invalid server port");
        requestedPort = port;
        roomExecutor = new ScheduledThreadPoolExecutor(1, daemonFactory("tetris-room"));
        roomExecutor.setRemoveOnCancelPolicy(true);
    }

    /** 루프백 주소만 바인딩하고 단일 방 작업 큐의 중력 시계 시작 */
    public synchronized void start() throws IOException {
        if (closed) throw new IllegalStateException("Server is closed");
        if (listener != null) return;
        ServerSocket socket = new ServerSocket();
        try {
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), requestedPort));
        } catch (IOException failure) {
            socket.close();
            throw failure;
        }
        listener = socket;
        roomExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() { tickRooms(); }
        }, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
        acceptThread = daemonFactory("tetris-accept").newThread(new Runnable() {
            public void run() { acceptLoop(); }
        });
        acceptThread.start();
    }

    public int getPort() {
        ServerSocket socket = listener;
        return socket == null ? requestedPort : socket.getLocalPort();
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = listener.accept();
                socket.setTcpNoDelay(true);
                if (closed || peers.size() >= MAX_CONNECTIONS) {
                    socket.close();
                    continue;
                }
                Peer peer = new Peer(socket);
                peers.put(peer, Boolean.TRUE);
                peer.start();
            } catch (SocketException stopped) {
                if (closed) return;
            } catch (IOException failure) {
                if (closed) return;
            }
        }
    }

    /** 한 작업자에서 모든 방의 중력 입력과 결과 배포 */
    private void tickRooms() {
        for (Room room : new ArrayList<Room>(rooms.values())) {
            if (room.battle == null || room.battle.getState().getStatus() != BattleState.Status.RUNNING) continue;
            try {
                room.battle.tick();
                broadcastSnapshot(room);
            } catch (RuntimeException failure) {
                for (Peer peer : room.members.values()) peer.send(NetworkUpdate.error("SERVER_TICK_FAILED"));
            }
        }
    }

    private void handle(Peer peer, WireRequest request) {
        if (peer.disconnected.get()) return;
        if (request.getRequestId() <= peer.lastRequestId) {
            outcome(peer, request.getRequestId(), false, "REQUEST_ID_NOT_INCREASING");
            return;
        }
        peer.lastRequestId = request.getRequestId();
        switch (request.getKind()) {
            case ROOM: handleRoom(peer, request.getRequestId(), request.getRoomCommand()); break;
            case INTENT: handleIntent(peer, request); break;
            case SNAPSHOT: handleSnapshot(peer, request); break;
            default: outcome(peer, request.getRequestId(), false, "UNKNOWN_REQUEST"); break;
        }
    }

    private void handleRoom(Peer peer, long requestId, RoomCommand command) {
        if (command == null) {
            outcome(peer, requestId, false, "INVALID_ROOM_COMMAND");
            return;
        }
        switch (command.getType()) {
            case CREATE_ROOM: createRoom(peer, requestId, command); break;
            case JOIN_ROOM: joinRoom(peer, requestId, command.getRoomId()); break;
            case GET_ROOM_STATE:
                if (peer.room == null) { outcome(peer, requestId, false, "NOT_IN_ROOM"); break; }
                outcome(peer, requestId, true, null);
                sendRoomState(peer);
                break;
            case SET_READY: setReady(peer, requestId, command.getReady()); break;
            case REQUEST_REMATCH: requestRematch(peer, requestId); break;
            case LEAVE_ROOM: leaveRoom(peer, requestId); break;
            default: outcome(peer, requestId, false, "UNKNOWN_ROOM_COMMAND"); break;
        }
    }

    private void createRoom(Peer peer, long requestId, RoomCommand command) {
        if (peer.room != null) { outcome(peer, requestId, false, "ALREADY_IN_ROOM"); return; }
        if (command.getMaxParticipants() == null || command.getMaxParticipants().intValue() != 2) {
            outcome(peer, requestId, false, "ROOM_SIZE_NOT_SUPPORTED");
            return;
        }
        Room room = new Room();
        rooms.put(room.id, room);
        room.members.put(peer.id, peer);
        room.ready.put(peer.id, Boolean.FALSE);
        room.version++;
        peer.room = room;
        outcome(peer, requestId, true, null);
        sendRoomState(peer);
    }

    private void joinRoom(Peer peer, long requestId, String roomId) {
        if (peer.room != null) { outcome(peer, requestId, false, "ALREADY_IN_ROOM"); return; }
        Room room = rooms.get(roomId);
        if (room == null) { outcome(peer, requestId, false, "ROOM_NOT_FOUND"); return; }
        if (room.members.size() >= 2) { outcome(peer, requestId, false, "ROOM_FULL"); return; }
        if (room.battle != null && room.battle.getState().getStatus() == BattleState.Status.RUNNING) {
            outcome(peer, requestId, false, "MATCH_RUNNING"); return;
        }
        // 종료된 경기의 새 참가자는 새 준비 라운드와 새 경기 ID부터 시작
        room.battle = null;
        room.matchId = null;
        for (String id : room.ready.keySet()) room.ready.put(id, Boolean.FALSE);
        room.members.put(peer.id, peer);
        room.ready.put(peer.id, Boolean.FALSE);
        room.version++;
        peer.room = room;
        outcome(peer, requestId, true, null);
        broadcastRoomState(room);
    }

    private void setReady(Peer peer, long requestId, Boolean ready) {
        Room room = peer.room;
        if (room == null) { outcome(peer, requestId, false, "NOT_IN_ROOM"); return; }
        if (ready == null) { outcome(peer, requestId, false, "INVALID_READY"); return; }
        if (room.battle != null && room.battle.getState().getStatus() == BattleState.Status.RUNNING) {
            outcome(peer, requestId, false, "MATCH_RUNNING"); return;
        }
        room.ready.put(peer.id, ready);
        room.version++;
        outcome(peer, requestId, true, null);
        broadcastRoomState(room);
        if (allReady(room)) startMatch(room);
    }

    private void requestRematch(Peer peer, long requestId) {
        Room room = peer.room;
        if (room == null) { outcome(peer, requestId, false, "NOT_IN_ROOM"); return; }
        if (room.battle == null || room.battle.getState().getStatus() != BattleState.Status.FINISHED) {
            outcome(peer, requestId, false, "MATCH_NOT_FINISHED"); return;
        }
        setReady(peer, requestId, Boolean.TRUE);
    }

    private boolean allReady(Room room) {
        return room.members.size() == 2 && room.ready.size() == 2
                && !room.ready.containsValue(Boolean.FALSE);
    }

    private void startMatch(Room room) {
        List<ParticipantSpec> specs = new ArrayList<ParticipantSpec>();
        int number = 1;
        for (Peer member : room.members.values()) {
            specs.add(new ParticipantSpec(member.id, "학생 " + number++, 100));
            room.ready.put(member.id, Boolean.FALSE);
        }
        BattleManager battle = new BattleManager(specs, System.nanoTime());
        BattleResult started = battle.start();
        if (!started.isAccepted()) {
            for (Peer member : room.members.values()) member.send(NetworkUpdate.error("MATCH_START_FAILED"));
            return;
        }
        room.battle = battle;
        room.matchId = UUID.randomUUID().toString();
        room.version++;
        broadcastRoomState(room);
        for (Peer member : room.members.values()) {
            member.send(NetworkUpdate.matchStarted(room.id, room.version, room.matchId, member.id));
        }
        broadcastSnapshot(room);
    }

    private void handleIntent(Peer peer, WireRequest request) {
        Room room = peer.room;
        if (room == null) { outcome(peer, request.getRequestId(), false, "NOT_IN_ROOM"); return; }
        if (room.matchId == null || !room.matchId.equals(request.getMatchId())) {
            outcome(peer, request.getRequestId(), false, "STALE_MATCH"); return;
        }
        if (room.battle == null || room.battle.getState().getStatus() != BattleState.Status.RUNNING) {
            outcome(peer, request.getRequestId(), false, "MATCH_NOT_RUNNING"); return;
        }
        PlayerIntent intent = request.getIntent();
        if (intent == null) { outcome(peer, request.getRequestId(), false, "INVALID_INTENT"); return; }
        long beforeVersion = room.battle.getState().getVersion();
        BattleResult result = intent.getType() == GameAction.Type.USE_ITEM
                ? room.battle.submitItem(peer.id, intent.getItemUse())
                : room.battle.submit(peer.id, intent.getType());
        outcome(peer, request.getRequestId(), result.isAccepted(),
                result.isAccepted() ? null : result.getReason());
        if (result.getState().getVersion() != beforeVersion) broadcastSnapshot(room);
    }

    private void handleSnapshot(Peer peer, WireRequest request) {
        Room room = peer.room;
        if (room == null) { outcome(peer, request.getRequestId(), false, "NOT_IN_ROOM"); return; }
        if (room.matchId == null || !room.matchId.equals(request.getMatchId())) {
            outcome(peer, request.getRequestId(), false, "STALE_MATCH"); return;
        }
        outcome(peer, request.getRequestId(), true, null);
        peer.send(NetworkUpdate.snapshot(room.id, room.matchId, room.battle.getState()));
    }

    private void leaveRoom(Peer peer, long requestId) {
        if (peer.room == null) { outcome(peer, requestId, false, "NOT_IN_ROOM"); return; }
        outcome(peer, requestId, true, null);
        Room room = peer.room;
        peer.send(NetworkUpdate.roomState(new RoomState(room.id, room.version + 1,
                RoomState.Phase.CLOSED, peer.id, room.ready)));
        removeFromRoom(peer);
    }

    /** 소켓에 결합된 참가자만 탈락시키고 남은 연결에 확정 결과 배포 */
    private void removeFromRoom(Peer peer) {
        Room room = peer.room;
        if (room == null) return;
        peer.room = null;
        if (room.battle != null && room.battle.getState().getStatus() == BattleState.Status.RUNNING) {
            room.battle.forfeit(peer.id);
        }
        room.members.remove(peer.id);
        room.ready.remove(peer.id);
        room.version++;
        if (room.members.isEmpty()) {
            rooms.remove(room.id);
        } else {
            if (room.battle != null) broadcastSnapshot(room);
            broadcastRoomState(room);
        }
    }

    private void outcome(Peer peer, long requestId, boolean accepted, String reason) {
        Room room = peer.room;
        String roomId = room == null ? null : room.id;
        String matchId = room == null ? null : room.matchId;
        Long version = room == null || room.battle == null ? null
                : Long.valueOf(room.battle.getState().getVersion());
        peer.send(NetworkUpdate.requestOutcome(new RequestOutcome(requestId, accepted,
                reason, roomId, matchId, version)));
    }

    private void broadcastRoomState(Room room) {
        for (Peer member : room.members.values()) sendRoomState(member);
    }

    private void sendRoomState(Peer member) {
        Room room = member.room;
        if (room == null) return;
        RoomState.Phase phase = room.battle == null ? RoomState.Phase.WAITING : RoomState.Phase.IN_MATCH;
        member.send(NetworkUpdate.roomState(new RoomState(room.id, room.version, phase,
                member.id, room.ready)));
    }

    private void broadcastSnapshot(Room room) {
        if (room.battle == null || room.matchId == null) return;
        NetworkUpdate snapshot = NetworkUpdate.snapshot(room.id, room.matchId, room.battle.getState());
        for (Peer member : room.members.values()) member.send(snapshot);
    }

    private void disconnect(Peer peer) {
        if (!peer.disconnected.compareAndSet(false, true)) return;
        peer.closeTransport();
        peers.remove(peer);
        if (!closed) {
            try {
                roomExecutor.execute(new Runnable() {
                    public void run() { removeFromRoom(peer); }
                });
            } catch (RejectedExecutionException ignored) { }
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        ServerSocket socket = listener;
        if (socket != null) try { socket.close(); } catch (IOException ignored) { }
        for (Peer peer : peers.keySet()) {
            peer.disconnected.set(true);
            peer.closeTransport();
        }
        peers.clear();
        roomExecutor.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Usage: LocalGameServer [port]");
        int port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
        final LocalGameServer server = new LocalGameServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() { server.close(); }
        }, "tetris-server-shutdown"));
        System.out.println("Local PvP server listening on 127.0.0.1:" + server.getPort());
        new java.util.concurrent.CountDownLatch(1).await();
    }

    private static ThreadFactory daemonFactory(final String name) {
        return new ThreadFactory() {
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, name);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    private static final class Room {
        private final String id = UUID.randomUUID().toString();
        private final Map<String, Peer> members = new LinkedHashMap<String, Peer>();
        private final Map<String, Boolean> ready = new LinkedHashMap<String, Boolean>();
        private long version;
        private String matchId;
        private BattleManager battle;
    }

    private final class Peer {
        private final String id = "player-" + UUID.randomUUID().toString();
        private final Socket socket;
        private final ArrayBlockingQueue<NetworkUpdate> outbound =
                new ArrayBlockingQueue<NetworkUpdate>(MAX_PENDING_UPDATES);
        private final Semaphore pendingRequests = new Semaphore(MAX_PENDING_REQUESTS);
        private final AtomicBoolean disconnected = new AtomicBoolean();
        private volatile Room room;
        private volatile Thread readerThread;
        private volatile Thread writerThread;
        private long lastRequestId;

        private Peer(Socket socket) { this.socket = socket; }

        private void start() {
            writerThread = daemonFactory("tetris-peer-writer").newThread(new Runnable() {
                public void run() {
                    try {
                        while (!disconnected.get()) {
                            NetworkUpdate update = outbound.take();
                            WireCodec.writeUpdate(socket.getOutputStream(), update);
                        }
                    } catch (IOException failed) { disconnect(Peer.this); }
                    catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                }
            });
            readerThread = daemonFactory("tetris-peer-reader").newThread(new Runnable() {
                public void run() {
                    try {
                        while (!disconnected.get()) {
                            final WireRequest request = WireCodec.readRequest(socket.getInputStream());
                            if (request == null) break;
                            if (!pendingRequests.tryAcquire()) break;
                            try {
                                roomExecutor.execute(new Runnable() {
                                    public void run() {
                                        try { handle(Peer.this, request); }
                                        finally { pendingRequests.release(); }
                                    }
                                });
                            } catch (RejectedExecutionException stopped) {
                                pendingRequests.release();
                                break;
                            }
                        }
                    } catch (IOException failed) { }
                    finally { disconnect(Peer.this); }
                }
            });
            writerThread.start();
            send(NetworkUpdate.connected());
            readerThread.start();
        }

        private void send(NetworkUpdate update) {
            if (!disconnected.get() && !outbound.offer(update)) disconnect(this);
        }

        private void closeTransport() {
            try { socket.close(); } catch (IOException ignored) { }
            Thread reader = readerThread;
            Thread writer = writerThread;
            if (reader != null) reader.interrupt();
            if (writer != null) writer.interrupt();
        }
    }
}
