package kr.ac.jbnu.se.tetris.app;

import java.awt.Component;
import java.awt.Container;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.app.session.SessionPhase;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.server.LocalGameServer;

/** 현재 화면에서 서버 거절, 재대결 중 퇴장, 대체 참가자 입장을 검증한다. */
public final class OnlineUiFlowTest {
    private OnlineUiFlowTest() { }

    public static void main(String[] args) throws Exception {
        SeongeunApplication[] apps = new SeongeunApplication[3];
        try (LocalGameServer server = new LocalGameServer(0)) {
            server.start();
            try {
                onEdt(() -> {
                    for (int i = 0; i < 2; i++) {
                        apps[i] = new SeongeunApplication(new Random(i + 401), null);
                        apps[i].openOnline(new ConnectionOptions("127.0.0.1", server.getPort()));
                    }
                });
                await(() -> apps[0].isOnlineConnected() && apps[1].isOnlineConnected(), "connected lobby");
                onEdt(() -> apps[0].createRoomWithName("회귀 테스트"));
                await(() -> apps[0].getRoomState() != null, "room created");
                String roomId = onEdtString(() -> apps[0].getRoomState().getRoomId());
                onEdt(() -> apps[1].joinRoom(roomId));
                await(() -> apps[1].getRoomState() != null
                        && apps[1].getRoomState().getReadyByParticipantId().size() == 2,
                        "both participants in waiting room");
                onEdt(() -> {
                    check("WAITING_ROOM".equals(apps[0].getCurrentScreen()), "host waiting screen");
                    check("WAITING_ROOM".equals(apps[1].getCurrentScreen()), "guest waiting screen");
                    ready(apps[0]); ready(apps[1]);
                });
                await(() -> running(apps[0]) && running(apps[1]), "first match running");
                onEdt(() -> apps[0].submit(GameAction.Type.HOLD));
                await(() -> apps[0].getMatchSnapshot().getBattleState()
                                .getParticipant(apps[0].getRoomState().getLocalParticipantId())
                                .getGameState().getHoldPiece() != null,
                        "server hold rendered");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                while (!onEdtBoolean(() -> finished(apps[0])) && System.nanoTime() < deadline) {
                    onEdt(() -> apps[0].submit(GameAction.Type.HARD_DROP));
                    Thread.sleep(50);
                }
                await(() -> finished(apps[0]) && finished(apps[1]), "both results delivered");
                onEdt(() -> {
                    button(apps[0], "대기방으로").doClick();
                    apps[0].createRoomWithName("중복 요청");
                });
                await(() -> apps[0].getLastMessage() != null
                        && apps[0].getLastMessage().contains("ALREADY_IN_ROOM"),
                        "unrelated create request rejected");
                onEdt(() -> {
                    check("WAITING_ROOM".equals(apps[0].getCurrentScreen()), "rejection retains original room");
                    button(apps[1], "대기방으로").doClick();
                });
                await(() -> "WAITING_ROOM".equals(apps[1].getCurrentScreen()), "opponent returned to room");
                onEdt(() -> { ready(apps[0]); ready(apps[1]); });
                await(() -> running(apps[0]) && running(apps[1]), "rematch running");
                onEdt(() -> button(apps[0], "대전 포기 [ESC]").doClick());
                await(() -> "LOBBY".equals(apps[0].getCurrentScreen()) && finished(apps[1]),
                        "leaving client home and opponent result");
                onEdt(() -> {
                    check("FORFEIT".equals(apps[1].getMatchSnapshot().getBattleState().getReason()),
                            "disconnect forfeits");
                    apps[2] = new SeongeunApplication(new Random(403), null);
                    apps[2].openOnline(new ConnectionOptions("127.0.0.1", server.getPort()));
                });
                await(() -> apps[2].isOnlineConnected(), "replacement connected");
                onEdt(() -> apps[2].joinRoom(roomId));
                await(() -> apps[2].getRoomState() != null
                        && apps[2].getRoomState().getReadyByParticipantId().size() == 2,
                        "replacement joined original room");
                onEdt(() -> button(apps[1], "대기방으로").doClick());
                await(() -> "WAITING_ROOM".equals(apps[1].getCurrentScreen())
                        && "WAITING_ROOM".equals(apps[2].getCurrentScreen()),
                        "original participant sees replacement");
                onEdt(() -> { ready(apps[1]); ready(apps[2]); });
                await(() -> running(apps[1]) && running(apps[2]), "replacement match running");
            } finally {
                onEdt(() -> { for (SeongeunApplication app : apps) if (app != null) app.close(); });
            }
        }
        System.out.println("PASS OnlineUiFlowTest: current UI rejection and replacement peer");
    }

    private static boolean running(SeongeunApplication app) {
        return "BATTLE".equals(app.getCurrentScreen()) && app.getMatchSnapshot() != null
                && app.getMatchSnapshot().getPhase() == SessionPhase.RUNNING;
    }
    private static boolean finished(SeongeunApplication app) {
        return "RESULT".equals(app.getCurrentScreen()) && app.getMatchSnapshot() != null
                && app.getMatchSnapshot().getPhase() == SessionPhase.FINISHED;
    }
    private static void ready(SeongeunApplication app) {
        AbstractButton control = findButton(app.getScreens(), null, "waitingReady");
        check(control != null && control.isEnabled(), "ready control enabled");
        control.doClick();
    }
    private static AbstractButton button(SeongeunApplication app, String label) {
        AbstractButton control = findButton(app.getScreens(), label, null);
        check(control != null, "button visible: " + label);
        return control;
    }
    private static AbstractButton findButton(Component root, String label, String name) {
        if (root instanceof AbstractButton) {
            AbstractButton result = (AbstractButton) root;
            if (label != null ? label.equals(result.getText()) : name.equals(result.getName())) return result;
        }
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            AbstractButton found = findButton(child, label, name);
            if (found != null) return found;
        }
        return null;
    }
    private static boolean onEdtBoolean(BooleanSupplier predicate) throws Exception {
        AtomicBoolean value = new AtomicBoolean();
        onEdt(() -> value.set(predicate.getAsBoolean()));
        return value.get();
    }
    private static String onEdtString(java.util.function.Supplier<String> supplier) throws Exception {
        String[] value = new String[1]; onEdt(() -> value[0] = supplier.get()); return value[0];
    }
    private static void await(BooleanSupplier predicate, String label) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onEdtBoolean(predicate)) return;
            Thread.sleep(10);
        }
        throw new AssertionError(label);
    }
    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
