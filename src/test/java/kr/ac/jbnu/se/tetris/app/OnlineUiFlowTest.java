package kr.ac.jbnu.se.tetris.app;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.server.LocalGameServer;

/** 실제 서버와 두 앱의 로비·대전·결과·재대결·나가기 연결 검증 */
public final class OnlineUiFlowTest {
    public static void main(String[] args) throws Exception {
        TetrisApplication[] apps = new TetrisApplication[3];
        try (LocalGameServer server = new LocalGameServer(0)) {
            server.start();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (int i = 0; i < 2; i++) {
                        apps[i] = new TetrisApplication();
                        button(apps[i], "onlinePvP").doClick();
                        field(apps[i], "serverPort").setText(Integer.toString(server.getPort()));
                        button(apps[i], "connectServer").doClick();
                    }
                });
                awaitEdt(() -> button(apps[0], "createRoom").isEnabled() && button(apps[1], "createRoom").isEnabled(), "connected lobby");
                SwingUtilities.invokeAndWait(() -> button(apps[0], "createRoom").doClick());
                awaitEdt(() -> !field(apps[0], "roomId").getText().isEmpty(), "room ID shown");
                SwingUtilities.invokeAndWait(() -> {
                    field(apps[1], "roomId").setText(field(apps[0], "roomId").getText());
                    button(apps[1], "joinRoom").doClick();
                });
                awaitEdt(() -> button(apps[1], "roomReady").isEnabled(), "joined room ready control");
                SwingUtilities.invokeAndWait(() -> { button(apps[0], "roomReady").doClick(); button(apps[1], "roomReady").doClick(); });
                awaitEdt(() -> running(apps[0]) && running(apps[1]), "two battle screens");
                SwingUtilities.invokeAndWait(() -> {
                    check(!apps[0].isGravityRunning() && !apps[0].isAiTimerRunning(), "online owns no local tick or AI timer");
                    check(!button(apps[0], "battlePause").isEnabled(), "pause disabled");
                    apps[0].submit(GameAction.Type.HOLD);
                });
                awaitEdt(() -> apps[0].getState().getHoldPiece() != null, "server hold rendered");
                long deadline = System.nanoTime() + 15_000_000_000L;
                while (!onEdt(() -> finished(apps[0])) && System.nanoTime() < deadline) {
                    SwingUtilities.invokeAndWait(() -> apps[0].submit(GameAction.Type.HARD_DROP));
                    Thread.sleep(50);
                }
                awaitEdt(() -> finished(apps[0]) && finished(apps[1]), "top out result screens");
                SwingUtilities.invokeAndWait(() -> {
                    button(apps[0], "retry").doClick();
                    apps[0].sendRoomCommand(RoomCommand.createRoom(2));
                });
                awaitEdt(() -> ((JLabel) find(apps[0].getRouter().getContainer(), "onlineStatus"))
                        .getText().contains("ALREADY_IN_ROOM"), "unrelated request rejection delivered");
                SwingUtilities.invokeAndWait(() -> {
                    check(!button(apps[0], "retry").isEnabled(), "unrelated rejection preserves rematch pending");
                    button(apps[1], "retry").doClick();
                });
                awaitEdt(() -> running(apps[0]) && running(apps[1]), "rematch battle screens");
                SwingUtilities.invokeAndWait(() -> apps[0].showHome());
                awaitEdt(() -> finished(apps[1]), "leaving opponent result");
                SwingUtilities.invokeAndWait(() -> {
                    check("home".equals(apps[0].getRouter().getCurrentId()), "leaving client stays home");
                    check("FORFEIT".equals(apps[1].getBattleState().getReason()), "disconnect forfeits");
                    apps[2] = new TetrisApplication();
                    apps[2].connectOnline(new ConnectionOptions("127.0.0.1", server.getPort()));
                });
                awaitEdt(() -> button(apps[2], "createRoom").isEnabled(), "replacement participant connected");
                SwingUtilities.invokeAndWait(() -> apps[2].sendRoomCommand(RoomCommand.joinRoom(field(apps[1], "roomId").getText())));
                awaitEdt(() -> "online".equals(apps[1].getRouter().getCurrentId())
                        && button(apps[1], "roomReady").isEnabled()
                        && button(apps[2], "roomReady").isEnabled(), "existing participant returns to lobby for new opponent");
                SwingUtilities.invokeAndWait(() -> { button(apps[1], "roomReady").doClick(); button(apps[2], "roomReady").doClick(); });
                awaitEdt(() -> running(apps[1]) && running(apps[2]), "new opponent starts new match");
            } finally {
                SwingUtilities.invokeAndWait(() -> { for (TetrisApplication app : apps) if (app != null) app.close(); });
            }
        }
        System.out.println("PASS OnlineUiFlowTest: real socket lobby, match, result, rematch and disconnect without native windows");
    }
    private static boolean running(TetrisApplication app) {
        return "battle".equals(app.getRouter().getCurrentId()) && app.getBattleState() != null
                && app.getBattleState().getStatus() == BattleState.Status.RUNNING;
    }
    private static boolean finished(TetrisApplication app) {
        return "result".equals(app.getRouter().getCurrentId()) && app.getBattleState() != null
                && app.getBattleState().getStatus() == BattleState.Status.FINISHED;
    }
    private static Component find(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container) {
                Component found = find((Container) component, name); if (found != null) return found;
            }
        }
        return null;
    }
    private static JButton button(TetrisApplication app, String name) { return (JButton) find(app.getRouter().getContainer(), name); }
    private static JTextField field(TetrisApplication app, String name) { return (JTextField) find(app.getRouter().getContainer(), name); }
    private static boolean onEdt(BooleanSupplier predicate) throws Exception {
        AtomicBoolean result = new AtomicBoolean(); SwingUtilities.invokeAndWait(() -> result.set(predicate.getAsBoolean())); return result.get();
    }
    private static void awaitEdt(BooleanSupplier predicate, String message) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!onEdt(predicate) && System.nanoTime() < deadline) Thread.sleep(10);
        check(onEdt(predicate), message);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
