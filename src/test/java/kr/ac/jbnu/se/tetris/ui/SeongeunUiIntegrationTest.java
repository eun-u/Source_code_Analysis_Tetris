package kr.ac.jbnu.se.tetris.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.app.TetrisApplication;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

/** 이식된 화면의 실제 입력·잠금·HP·서버 준비 상태 연결 검증 */
public final class SeongeunUiIntegrationTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            stageRowsRespectProgress();
            roomCardsFollowServerState();
            appControlsUseCurrentSession();
        });
        System.out.println("PASS SeongeunUiIntegrationTest: stage locks, room cards, HP and live session controls");
    }

    private static void stageRowsRespectProgress() {
        StageCatalog catalog = StageCatalog.loadDefault();
        Set<String> unlocked = new HashSet<String>();
        String first = catalog.getStages().get(0).getId();
        String second = catalog.getStages().get(1).getId();
        unlocked.add(first);
        List<String> started = new ArrayList<String>();
        StageSelectPanel panel = new StageSelectPanel(catalog, unlocked::contains, started::add, () -> { });
        for (int i = 0; i < catalog.getStages().size(); i++) {
            JButton row = button(panel, "stage-" + catalog.getStages().get(i).getId());
            check(row.isEnabled() == (i == 0), "all catalog stages reflect lock state");
        }
        JComboBox<?> selection = (JComboBox<?>) find(panel, "stageSelection");
        selection.setSelectedIndex(1);
        check(!button(panel, "beginStory").isEnabled(), "locked selection cannot begin");
        button(panel, "beginStory").doClick();
        check(started.isEmpty(), "locked stage sends no start intent");
        unlocked.add(second);
        panel.onEnter();
        button(panel, "stage-" + second).doClick();
        check(selection.getSelectedIndex() == 1, "stage row and selection model agree");
        button(panel, "beginStory").doClick();
        check(started.size() == 1 && second.equals(started.get(0)), "row starts stable stage ID");
    }

    private static void roomCardsFollowServerState() {
        List<RoomCommand> sent = new ArrayList<RoomCommand>();
        RoomPanel panel = new RoomPanel(sent::add);
        Map<String, Boolean> ready = new LinkedHashMap<String, Boolean>();
        ready.put("me", false); ready.put("opponent", true);
        panel.setState(new RoomState("room-1", 1, RoomState.Phase.WAITING, "me", ready));
        check(find(panel, "roomParticipant-me") != null && find(panel, "roomParticipant-opponent") != null,
                "cards follow participant IDs");
        button(panel, "roomReady").doClick();
        button(panel, "roomReady").doClick();
        check(sent.size() == 2 && Boolean.TRUE.equals(sent.get(0).getReady())
                && Boolean.TRUE.equals(sent.get(1).getReady()), "ready clicks await server confirmation");
        ready.put("me", true); ready.remove("opponent");
        panel.setState(new RoomState("room-1", 2, RoomState.Phase.WAITING, "me", ready));
        check(find(panel, "roomParticipant-opponent") == null, "departed participant card removed");
        button(panel, "roomReady").doClick();
        check(Boolean.FALSE.equals(sent.get(2).getReady()), "confirmed ready state sends cancel");
        panel.setState(new RoomState("room-1", 3, RoomState.Phase.IN_MATCH, "me", ready));
        check(!button(panel, "roomReady").isEnabled(), "match disables readiness changes");
        panel.setState(null);
        check(find(panel, "roomParticipant-me") == null && !button(panel, "leaveRoom").isEnabled(),
                "room reset clears participant cards and actions");
    }

    private static void appControlsUseCurrentSession() {
        TetrisApplication app = new TetrisApplication();
        Container root = app.getRouter().getContainer();
        try {
            button(root, "newGame").doClick();
            check("game".equals(app.getRouter().getCurrentId()), "lobby tutorial opens current game");
            GamePanel game = findType(root, GamePanel.class);
            long before = app.getState().getVersion();
            press(game, KeyEvent.VK_LEFT);
            check(app.getState().getVersion() > before, "game key binding reaches engine");
            button(root, "home").doClick();
            long hiddenVersion = app.getState().getVersion();
            press(game, KeyEvent.VK_RIGHT);
            check(!app.isGravityRunning() && app.getState().getVersion() == hiddenVersion,
                    "hidden game owns no timer or input advance");
            button(root, "newBattle").doClick();
            button(root, "beginStory").doClick();
            check("battle".equals(app.getRouter().getCurrentId()), "lobby story opens real battle");
            Container localHp = (Container) find(root, "participantHp-local");
            JProgressBar hp = findType(localHp, JProgressBar.class);
            check(hp.getValue() == app.getBattleState().getParticipant("local").getHp()
                    && hp.getMaximum() == app.getBattleState().getParticipant("local").getMaxHp(),
                    "HP component displays authoritative participant state");
            button(root, "battlePause").doClick();
            check(app.getBattleState().getStatus() == BattleState.Status.PAUSED
                    && !app.isGravityRunning() && !app.isAiTimerRunning(), "pause controls session clocks");
            button(root, "battleHome").doClick();
            button(root, "continueGame").doClick();
            check("battle".equals(app.getRouter().getCurrentId()) && app.isGravityRunning(),
                    "continue restores the same live session");
        } finally { app.close(); }
        check(!app.isGravityRunning() && !app.isAiTimerRunning(), "close releases clocks");
    }

    private static void press(JComponent panel, int key) {
        Object action = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(key, 0));
        check(action != null, "key action exists");
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, "integration"));
    }

    private static JButton button(Container root, String name) {
        Component component = find(root, name);
        check(component instanceof JButton, "button exists: " + name);
        return (JButton) component;
    }

    private static Component find(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container) {
                Component nested = find((Container) child, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static <T> T findType(Container root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) {
                T nested = findType((Container) child, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
