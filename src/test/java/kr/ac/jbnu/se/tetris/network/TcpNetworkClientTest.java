package kr.ac.jbnu.se.tetris.network;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import kr.ac.jbnu.se.tetris.app.session.*;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.server.LocalGameServer;

/** 실제 TCP 클라이언트와 온라인 세션의 두 참가자 통합 검증 */
public final class TcpNetworkClientTest {
    public static void main(String[] args) throws Exception {
        try (LocalGameServer server = new LocalGameServer(0)) {
            server.start();
            TcpNetworkClient first = new TcpNetworkClient();
            TcpNetworkClient second = new TcpNetworkClient();
            OnlineMatchSession one = new OnlineMatchSession("first", first);
            OnlineMatchSession two = new OnlineMatchSession("second", second);
            Probe a = new Probe(first), b = new Probe(second);
            Map<Long, CommandOutcome> outcomes = Collections.synchronizedMap(new LinkedHashMap<Long, CommandOutcome>());
            one.subscribe(update -> { if (update.getOutcome() != null) outcomes.put(update.getOutcome().getRequestId(), update.getOutcome()); });
            try {
                first.connect(new ConnectionOptions("127.0.0.1", server.getPort()));
                second.connect(new ConnectionOptions("127.0.0.1", server.getPort()));
                await(() -> a.connected && b.connected, "two real TCP connections");
                first.send(RoomCommand.createRoom(2));
                await(() -> a.room != null, "created room");
                second.send(RoomCommand.joinRoom(a.room.getRoomId()));
                await(() -> b.room != null && a.room.getReadyByParticipantId().size() == 2, "joined room");
                first.send(RoomCommand.setReady(true)); second.send(RoomCommand.setReady(true));
                await(() -> one.getSnapshot().getPhase() == SessionPhase.RUNNING
                        && two.getSnapshot().getPhase() == SessionPhase.RUNNING, "started match");
                check(!one.getSnapshot().getLocalParticipantId().equals(two.getSnapshot().getLocalParticipantId()), "server assigned distinct IDs");
                long action = one.submit(new PlayerIntent(GameAction.Type.HOLD));
                await(() -> outcomes.containsKey(action), "confirmed action outcome");
                check(outcomes.get(action).isAccepted(), "server accepted hold");
                String actor = one.getSnapshot().getLocalParticipantId();
                await(() -> two.getSnapshot().getBattleState().getParticipant(actor).getGameState().getHoldPiece() != null,
                        "other client received authoritative hold");
                long pause = one.requestPause(true);
                await(() -> outcomes.containsKey(pause), "pause rejected");
                check("PAUSE_NOT_ALLOWED".equals(outcomes.get(pause).getReasonCode()), "online cannot pause");
                second.close();
                await(() -> one.getSnapshot().getPhase() == SessionPhase.FINISHED, "disconnect forfeit");
                check(actor.equals(one.getSnapshot().getBattleState().getWinnerId()), "surviving participant wins");
                long leave = one.leave();
                await(() -> outcomes.containsKey(leave) && one.getSnapshot().getBattleState() == null, "leave result and cleared state");
                check(outcomes.get(leave).isAccepted(), "leave accepted");
                first.send(RoomCommand.createRoom(2));
                await(() -> a.room.getPhase() == RoomState.Phase.WAITING && a.room.getReadyByParticipantId().size() == 1,
                        "new room on same connection");
            } finally { one.close(); two.close(); first.close(); second.close(); }
        }
        connectionFailure();
        System.out.println("PASS TcpNetworkClientTest: real TCP, two sessions, inputs, disconnect, room reuse, failure");
    }

    private static void connectionFailure() throws Exception {
        int unused;
        try (ServerSocket reserved = new ServerSocket(0)) { unused = reserved.getLocalPort(); }
        TcpNetworkClient client = new TcpNetworkClient();
        AtomicReference<NetworkUpdate.Type> terminal = new AtomicReference<NetworkUpdate.Type>();
        client.subscribe(update -> {
            if (update.getType() == NetworkUpdate.Type.CONNECTION_FAILED) terminal.set(update.getType());
        });
        try {
            client.connect(new ConnectionOptions("127.0.0.1", unused));
            await(() -> terminal.get() != null, "connection failure callback");
            try { client.send(RoomCommand.createRoom(2)); throw new AssertionError("closed client accepted request"); }
            catch (IllegalStateException expected) { check("CLOSED".equals(expected.getMessage()), "closed rejection"); }
        } finally { client.close(); }
    }
    private static final class Probe {
        private volatile boolean connected;
        private volatile RoomState room;
        private Probe(TcpNetworkClient client) {
            client.subscribe(update -> {
                if (update.getType() == NetworkUpdate.Type.CONNECTED) connected = true;
                if (update.getType() == NetworkUpdate.Type.ROOM_STATE) room = update.getRoomState();
            });
        }
    }
    private static void await(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
        check(condition.getAsBoolean(), message);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
