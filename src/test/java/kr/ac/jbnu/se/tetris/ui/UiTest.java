package kr.ac.jbnu.se.tetris.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.app.TetrisApplication;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;

/** 창 생성 없는 화면·세션 계약 검증 및 실제 OS 입력 제외 */
public final class UiTest {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            routerLifecycle();
            keyContract();
            sessionLifecycle();
            battleLifecycle();
            tutorialAndStory();
        });
        boolean rejected = false;
        try { new ScreenRouter().register(screen("off-edt", new ArrayList<>())); }
        catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "off-EDT screen mutation rejected");
        System.out.println("PASS UiTest: " + checks + " checks");
    }
    private static void tutorialAndStory() {
        TetrisApplication app = new TetrisApplication();
        try {
            app.startNewGame(42);
            for (GameAction.Type action : new GameAction.Type[] {GameAction.Type.MOVE_LEFT,
                    GameAction.Type.ROTATE_RIGHT, GameAction.Type.SOFT_DROP,
                    GameAction.Type.HOLD, GameAction.Type.HARD_DROP}) app.submit(action);
            check("result".equals(app.getRouter().getCurrentId()), "튜토리얼 목표 달성 후 결과 전환");
            check(!app.isGravityRunning(), "튜토리얼 완료 후 시계 정지");
            app.showStages();
            check("stages".equals(app.getRouter().getCurrentId()), "스토리 단계 선택 화면");
            boolean locked = false;
            try { app.startStory(4, 42); } catch (IllegalStateException expected) { locked = true; }
            check(locked, "잠긴 스테이지 직접 진입 차단");
            app.startStory(0, 42);
            check(app.getBattleState().getParticipant("monster").getMaxHp() == 65,
                    "첫 Stage 기본 HP 설정 적용");
            app.showHome();
            check(!app.isAiThinking() && !app.isAiTimerRunning(), "스토리 홈 이동 시 worker 정리");
            app.continueGame();
            check("battle".equals(app.getRouter().getCurrentId()), "스토리 이어하기");
            for (int i = 0; i < 100 && app.getBattleState().getStatus() != BattleState.Status.FINISHED; i++) {
                app.submit(GameAction.Type.HARD_DROP);
            }
            check("result".equals(app.getRouter().getCurrentId()), "스토리 패배 결과");
            app.nextEncounter();
            check("result".equals(app.getRouter().getCurrentId()), "패배 시 다음 상대 진입 차단");
        } finally { app.close(); }
    }
    private static Screen screen(String id, List<String> calls) {
        return new Screen() {
            private final JPanel panel = new JPanel();
            @Override public String getId() { return id; }
            @Override public JPanel getPanel() { return panel; }
            @Override public void onEnter() { calls.add(id + "+"); }
            @Override public void onExit() { calls.add(id + "-"); }
        };
    }
    private static void routerLifecycle() {
        List<String> calls = new ArrayList<>();
        ScreenRouter router = new ScreenRouter();
        router.register(screen("a", calls));
        router.register(screen("b", calls));
        router.show("a"); router.show("a"); router.show("b"); router.close();
        check(calls.equals(Arrays.asList("a+", "a-", "b+", "b-")), "router transition order/idempotence");
        check(router.getCurrentId() == null, "router close clears active screen");
        boolean duplicate = false;
        try { router.register(screen("a", calls)); } catch (IllegalArgumentException expected) { duplicate = true; }
        check(duplicate, "duplicate screen rejected");
        boolean unknown = false;
        try { router.show("missing"); } catch (IllegalArgumentException expected) { unknown = true; }
        check(unknown, "unknown screen rejected");
    }
    private static void keyContract() {
        JPanel panel = new JPanel();
        List<GameAction.Type> actions = new ArrayList<>();
        List<String> navigation = new ArrayList<>();
        GameKeyBindings.install(panel, actions::add, () -> navigation.add("pause"), () -> navigation.add("home"));
        for (int key : new int[] {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
                KeyEvent.VK_D, KeyEvent.VK_SPACE, KeyEvent.VK_C, KeyEvent.VK_P, KeyEvent.VK_ESCAPE}) {
            Object id = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(key, 0));
            check(id != null, "window binding " + key);
            panel.getActionMap().get(id).actionPerformed(new ActionEvent(panel, 0, "test"));
        }
        check(actions.equals(Arrays.asList(GameAction.Type.MOVE_LEFT, GameAction.Type.MOVE_RIGHT,
                GameAction.Type.ROTATE_LEFT, GameAction.Type.ROTATE_RIGHT, GameAction.Type.SOFT_DROP,
                GameAction.Type.HARD_DROP, GameAction.Type.HOLD)), "legacy keys and HOLD mapping");
        check(navigation.equals(Arrays.asList("pause", "home")), "pause/home routing");
    }
    private static void battleLifecycle() {
        TetrisApplication app = new TetrisApplication();
        try {
            app.startBattle(42);
            check("battle".equals(app.getRouter().getCurrentId()), "battle route");
            check(app.isGravityRunning() && app.isAiTimerRunning(), "battle timers start");
            check(app.getBattleState().getParticipants().size() == 2, "two participant snapshots");
            GameState before = app.getState();
            app.submit(GameAction.Type.HOLD);
            check(app.getState().getHoldPiece() == before.getActivePiece().getType(), "battle HOLD uses core");
            long holdVersion = app.getState().getVersion();
            app.submit(GameAction.Type.HOLD);
            check(app.getState().getVersion() == holdVersion, "battle rejects second HOLD");
            app.showHome();
            check(app.getBattleState().getStatus() == BattleState.Status.PAUSED, "home pauses match");
            check(!app.isGravityRunning() && !app.isAiTimerRunning() && !app.isAiThinking(), "home stops AI and both timers");
            long pausedVersion = app.getBattleState().getVersion();
            app.submit(GameAction.Type.HARD_DROP);
            check(app.getBattleState().getVersion() == pausedVersion, "hidden battle input ignored");
            app.continueGame();
            check("battle".equals(app.getRouter().getCurrentId()) && app.isAiTimerRunning(), "continue resumes battle");
            for (int i = 0; i < 100 && app.getBattleState().getStatus() != BattleState.Status.FINISHED; i++) {
                app.submit(GameAction.Type.HARD_DROP);
            }
            check("result".equals(app.getRouter().getCurrentId()), "battle top-out result");
            check(!app.isGravityRunning() && !app.isAiTimerRunning(), "result freezes battle timers");
            app.startBattle(7);
            check(app.getBattleState().getParticipant("local").getHp() == 100, "battle restart resets HP");
            app.startNewGame(7);
            check(app.getBattleState() == null && !app.isAiTimerRunning(), "switch to solo closes AI session");
            app.startBattle(9);
            app.close();
            check(!app.isAiTimerRunning() && !app.isAiThinking(), "close releases AI work");
        } finally { app.close(); }
    }
    private static void sessionLifecycle() {
        TetrisApplication app = new TetrisApplication();
        try {
            check("home".equals(app.getRouter().getCurrentId()), "launch at home");
            check(!app.isGravityRunning() && app.getState() == null, "no hidden session at home");
            app.startNewGame(42);
            check("game".equals(app.getRouter().getCurrentId()) && app.isGravityRunning(), "new game starts timer");
            GameState first = app.getState();
            app.togglePause();
            check(app.getState().getStatus() == GameState.Status.PAUSED && !app.isGravityRunning(), "pause stops timer");
            long pausedVersion = app.getState().getVersion();
            app.submit(GameAction.Type.GRAVITY_TICK);
            check(app.getState().getVersion() == pausedVersion, "paused tick does not advance");
            app.togglePause();
            check(app.isGravityRunning(), "resume starts timer");
            app.showHome();
            check(app.getState().getStatus() == GameState.Status.PAUSED && !app.isGravityRunning(), "home suspends engine");
            long homeVersion = app.getState().getVersion();
            app.submit(GameAction.Type.HARD_DROP);
            check(app.getState().getVersion() == homeVersion, "home ignores hidden key/tick actions");
            app.continueGame();
            check("game".equals(app.getRouter().getCurrentId()) && app.isGravityRunning(), "continue returns to same session");
            check(app.getState().getPieceX() == first.getPieceX(), "continue preserves board position");
            for (int i = 0; i < 100 && app.getState().getStatus() != GameState.Status.GAME_OVER; i++) {
                app.submit(GameAction.Type.HARD_DROP);
            }
            check(app.getState().getStatus() == GameState.Status.GAME_OVER, "stack reaches top-out");
            check("result".equals(app.getRouter().getCurrentId()) && !app.isGravityRunning(), "gameover shows result and stops timer");
            long overVersion = app.getState().getVersion();
            app.continueGame(); app.submit(GameAction.Type.GRAVITY_TICK);
            check(app.getState().getVersion() == overVersion, "ended session cannot resume");
            app.startNewGame(42);
            check(app.getState().getVersion() == first.getVersion(), "restart creates fresh version stream");
            check(app.getState().getLinesCleared() == 0, "restart resets lines");
            BoardView view = new BoardView();
            view.setState(app.getState());
            for (int size : new int[] {1, 40, 320}) {
                view.setSize(size, size * 2);
                BufferedImage image = new BufferedImage(size, size * 2, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D graphics = image.createGraphics();
                try { view.paint(graphics); } finally { graphics.dispose(); }
            }
            check(true, "snapshot view handles resize");
            app.close(); app.close();
            check(!app.isGravityRunning(), "close is idempotent and stops timer");
            app.startNewGame(99);
            check(!app.isGravityRunning() && app.getRouter().getCurrentId() == null, "closed app cannot resurrect");
        } finally { app.close(); }
    }
}
