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
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.app.TetrisApplication;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.story.StageCatalog;
import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.Stage;

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
        String firstEncounter = catalog.getStages().get(0).getEncounters().get(0).getId();
        String secondEncounter = catalog.getStages().get(1).getEncounters().get(0).getId();
        unlocked.add(first + ":" + firstEncounter);
        List<String> started = new ArrayList<String>();
        StageSelectPanel panel = new StageSelectPanel(catalog,
                (id, encounterId) -> unlocked.contains(id + ":" + encounterId),
                (id, encounterId) -> started.add(id + ":" + encounterId), () -> { });
        for (Stage stage : catalog.getStages()) {
            for (MonsterSpec monster : stage.getEncounters()) {
                JButton row = button(panel, "stage-" + stage.getId() + "-" + monster.getId());
                check(row.isEnabled() == monster.getId().equals(firstEncounter),
                        "encounters reflect lock state");
            }
        }
        check(find(panel, "stageSelection") == null && find(panel, "beginStory") == null,
                "original encounter buttons replace selection and separate launch");
        button(panel, "stage-" + first + "-level_2_monster").doClick();
        check(started.isEmpty(), "locked stage sends no start intent");
        unlocked.add(second + ":" + secondEncounter);
        panel.onEnter();
        button(panel, "stage-" + second + "-" + secondEncounter).doClick();
        check(started.size() == 1 && (second + ":" + secondEncounter).equals(started.get(0)),
                "button starts stable encounter ID");
        check(button(panel, "stage-employment-level_7_monster")
                != button(panel, "stage-employment-level_8_monster"),
                "two consecutive Elite monsters retain distinct buttons");
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
        panel.setMatchFinished(true);
        check(button(panel, "roomReady").isEnabled(), "confirmed finished match allows waiting-room rematch readiness");
        panel.setMatchFinished(false);
        check(!button(panel, "roomReady").isEnabled(), "new running match disables readiness again");
        panel.setState(null);
        check(find(panel, "roomParticipant-me") == null && !button(panel, "leaveRoom").isEnabled(),
                "room reset clears participant cards and actions");
    }

    private static void appControlsUseCurrentSession() {
        TetrisApplication app = new TetrisApplication();
        Container root = app.getRouter().getContainer();
        try {
            check("login".equals(app.getRouter().getCurrentId()), "original login entry");
            button(root, "signUp").doClick();
            check("signup".equals(app.getRouter().getCurrentId()), "original signup navigation");
            button(root, "signUpBack").doClick();
            check(!button(root, "login").isEnabled(), "unconnected authentication does not report success");
            button(root, "localLogin").doClick();
            check("home".equals(app.getRouter().getCurrentId()), "original login to lobby");
            root.setSize(800, 600); layout(root);
            java.awt.Point storyButton = SwingUtilities.convertPoint(button(root, "newBattle"), 0, 0, root);
            java.awt.Point onlineButton = SwingUtilities.convertPoint(button(root, "onlinePvP"), 0, 0, root);
            java.awt.Point localButton = SwingUtilities.convertPoint(button(root, "newGame"), 0, 0, root);
            check(storyButton.y == onlineButton.y && onlineButton.y == localButton.y
                    && storyButton.x < onlineButton.x && onlineButton.x < localButton.x,
                    "original horizontal Story Online Local menu order");
            button(root, "newGame").doClick();
            check("local-mode".equals(app.getRouter().getCurrentId()), "original Local Mode selection");
            button(root, "tutorial").doClick();
            check("game".equals(app.getRouter().getCurrentId()), "lobby tutorial opens current game");
            GamePanel game = findType(root, GamePanel.class);
            long before = app.getState().getVersion();
            press(game, KeyEvent.VK_LEFT);
            check(app.getState().getVersion() > before, "game key binding reaches engine");
            button(root, "home").doClick();
            press(game, KeyEvent.VK_RIGHT);
            check(!app.isGravityRunning() && app.getState() == null,
                    "original Back closes game and hidden input cannot advance");
            button(root, "localBack").doClick();
            button(root, "newBattle").doClick();
            String first = StageCatalog.loadDefault().getStages().get(0).getId();
            button(root, "stage-" + first + "-level_1_monster").doClick();
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
            check("home".equals(app.getRouter().getCurrentId()) && app.getBattleState() == null
                    && !app.isGravityRunning() && !app.isAiTimerRunning(), "original lobby return closes unfinished battle");
            button(root, "newGame").doClick();
            button(root, "infinite").doClick();
            check("game".equals(app.getRouter().getCurrentId()) && app.isGravityRunning(), "Infinite starts real engine");
            for (int i = 0; i < 100 && "game".equals(app.getRouter().getCurrentId()); i++) {
                app.submit(kr.ac.jbnu.se.tetris.core.GameAction.Type.HARD_DROP);
            }
            check("result".equals(app.getRouter().getCurrentId()) && !app.isGravityRunning(), "Infinite TopOut result freezes clock");
            button(root, "retry").doClick();
            check("local-mode".equals(app.getRouter().getCurrentId()), "original result returns to Local Mode");
            button(root, "sprint").doClick();
            check(app.getState().getStatus() == kr.ac.jbnu.se.tetris.core.GameState.Status.RUNNING,
                    "Sprint starts fresh live engine");
        } finally { app.close(); }
        check(!app.isGravityRunning() && !app.isAiTimerRunning(), "close releases clocks");
    }

    private static void press(JComponent panel, int key) {
        Object action = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(key, 0));
        check(action != null, "key action exists");
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, "integration"));
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child.isVisible() && child instanceof Container) layout((Container) child);
        }
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
