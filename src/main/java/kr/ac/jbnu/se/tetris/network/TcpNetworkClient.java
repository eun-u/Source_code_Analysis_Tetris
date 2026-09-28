package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.protocol.WireCodec;
import kr.ac.jbnu.se.tetris.network.protocol.WireRequest;

/** 단일 연결의 비차단 송신·순서 보장 수신 및 서버 확정 상태 전달 */
public final class TcpNetworkClient implements NetworkClient {
    private final AtomicLong nextId = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ArrayBlockingQueue<WireRequest> outgoing = new ArrayBlockingQueue<WireRequest>(256);
    private final ThreadPoolExecutor callbacks = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(256), task -> daemon(task, "tetris-client-callback"));
    // 아래 구독 목록과 재생 사본은 콜백 실행기에서만 접근하는 상태
    private final List<Observer> observers = new ArrayList<Observer>();
    private NetworkUpdate connectedUpdate;
    private NetworkUpdate roomUpdate;
    private NetworkUpdate startUpdate;
    private NetworkUpdate snapshotUpdate;
    private volatile Socket socket;
    private volatile Thread writer;
    private volatile boolean connected;
    private volatile String matchId;

    @Override public long connect(ConnectionOptions options) {
        requireOpen();
        if (options == null) throw new IllegalArgumentException("Connection options required");
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("ALREADY_CONNECTING");
        long id = nextId.incrementAndGet();
        daemon(() -> readConnection(options), "tetris-client-reader").start();
        return id;
    }

    @Override public synchronized long send(RoomCommand command) {
        requireOpen();
        if (command == null) throw new IllegalArgumentException("Room command required");
        long id = nextId.incrementAndGet();
        enqueue(WireRequest.room(id, command));
        return id;
    }

    @Override public synchronized long send(PlayerIntent intent) {
        requireOpen();
        if (intent == null) throw new IllegalArgumentException("Player intent required");
        String current = matchId;
        if (current == null) throw new IllegalStateException("MATCH_NOT_STARTED");
        long id = nextId.incrementAndGet();
        enqueue(WireRequest.intent(id, current, intent));
        return id;
    }

    @Override public synchronized long requestSnapshot() {
        requireOpen();
        String current = matchId;
        if (current == null) throw new IllegalStateException("MATCH_NOT_STARTED");
        long id = nextId.incrementAndGet();
        enqueue(WireRequest.snapshot(id, current));
        return id;
    }

    private void enqueue(WireRequest request) {
        requireOpen();
        if (!connected) throw new IllegalStateException("NOT_CONNECTED");
        if (!outgoing.offer(request)) throw new IllegalStateException("SEND_QUEUE_FULL");
    }

    @Override public NetworkSubscription subscribe(NetworkListener listener) {
        requireOpen();
        if (listener == null) throw new IllegalArgumentException("Listener required");
        Observer observer = new Observer(listener);
        dispatch(() -> {
            if (!observer.active.get()) return;
            observers.add(observer);
            if (connectedUpdate != null) observer.deliver(connectedUpdate);
            if (roomUpdate != null) observer.deliver(roomUpdate);
            if (startUpdate != null) observer.deliver(startUpdate);
            if (snapshotUpdate != null) observer.deliver(snapshotUpdate);
        });
        return () -> {
            observer.active.set(false);
            if (!closed.get()) dispatch(() -> observers.remove(observer));
        };
    }

    private void readConnection(ConnectionOptions options) {
        try {
            Socket opened = new Socket();
            socket = opened;
            if (closed.get()) { opened.close(); return; }
            opened.connect(new InetSocketAddress(options.getHost(), options.getPort()), 3000);
            opened.setTcpNoDelay(true);
            if (closed.get()) { opened.close(); return; }
            connected = true;
            writer = daemon(() -> writeConnection(opened), "tetris-client-writer");
            writer.start();
            while (!closed.get()) {
                NetworkUpdate update = WireCodec.readUpdate(opened.getInputStream());
                if (update == null) { terminate("CONNECTION_LOST", false); return; }
                publish(update);
            }
        } catch (IOException | RuntimeException error) {
            if (!closed.get()) terminate(connected ? "CONNECTION_LOST" : "CONNECTION_FAILED", !connected);
        }
    }

    private void writeConnection(Socket opened) {
        try {
            while (!closed.get()) WireCodec.writeRequest(opened.getOutputStream(), outgoing.take());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException error) {
            if (!closed.get()) terminate("CONNECTION_LOST", false);
        }
    }

    private void publish(NetworkUpdate update) {
        dispatch(() -> {
            switch (update.getType()) {
                case CONNECTED: connectedUpdate = update; break;
                case ROOM_STATE:
                    roomUpdate = update;
                    if (update.getRoomState().getPhase() == RoomState.Phase.CLOSED
                            || (update.getRoomState().getPhase() == RoomState.Phase.WAITING
                                && startUpdate != null && update.getRoomVersion() > startUpdate.getRoomVersion())) {
                        matchId = null; startUpdate = null; snapshotUpdate = null;
                    }
                    break;
                case MATCH_STARTED:
                    matchId = update.getMatchId(); startUpdate = update; snapshotUpdate = null; break;
                case SNAPSHOT: snapshotUpdate = update; break;
                default: break;
            }
            for (Observer observer : new ArrayList<Observer>(observers)) observer.deliver(update);
        });
    }

    private void dispatch(Runnable action) {
        if (closed.get()) return;
        try { callbacks.execute(action); }
        catch (RejectedExecutionException overflow) {
            if (!closed.get()) terminate("RECEIVE_QUEUE_FULL", false);
        }
    }

    private void terminate(String reason, boolean connectionFailure) {
        if (!closed.compareAndSet(false, true)) return;
        connected = false;
        Socket current = socket;
        if (current != null) try { current.close(); } catch (IOException ignored) { }
        Thread currentWriter = writer;
        if (currentWriter != null) currentWriter.interrupt();
        outgoing.clear();
        // 수신 순서 뒤 종료 통지 및 내부 잠금을 보유한 콜백 호출 제외
        Runnable terminal = () -> {
            NetworkUpdate update = connectionFailure ? NetworkUpdate.connectionFailed(reason) : NetworkUpdate.closed(reason);
            for (Observer observer : new ArrayList<Observer>(observers)) observer.deliver(update);
            observers.clear();
            connectedUpdate = null; roomUpdate = null; startUpdate = null; snapshotUpdate = null;
        };
        try {
            callbacks.execute(terminal);
        } catch (RejectedExecutionException overflow) {
            // 수신 한도 초과 시 가장 오래된 대기 작업을 제거한 종료 통지 공간 확보
            callbacks.getQueue().poll();
            try { callbacks.execute(terminal); } catch (RejectedExecutionException ignored) { }
        }
        callbacks.shutdown();
    }

    private void requireOpen() { if (closed.get()) throw new IllegalStateException("CLOSED"); }
    @Override public void close() { terminate("CLIENT_CLOSED", false); }
    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name); thread.setDaemon(true); return thread;
    }
    private static final class Observer {
        private final NetworkListener listener;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private Observer(NetworkListener listener) { this.listener = listener; }
        private void deliver(NetworkUpdate update) {
            if (!active.get()) return;
            try { listener.onUpdate(update); }
            catch (RuntimeException error) {
                java.util.logging.Logger.getLogger(TcpNetworkClient.class.getName())
                        .log(java.util.logging.Level.WARNING, "Network listener failed", error);
            }
        }
    }
}
