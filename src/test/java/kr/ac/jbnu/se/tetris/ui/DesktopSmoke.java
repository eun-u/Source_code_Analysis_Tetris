package kr.ac.jbnu.se.tetris.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.app.TetrisApplication;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 실제 창과 OS 키 입력 검증을 위한 GUI 환경 필요 */
public final class DesktopSmoke {
    private static TetrisApplication app;
    private static MainWindow window;
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
        System.out.println("PASS desktop: " + name);
    }
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(35);
        File output = new File(args.length == 0 ? "." : args[0]);
        try {
            SwingUtilities.invokeAndWait(() -> {
                app = new TetrisApplication();
                window = new MainWindow(app.getRouter(), app::close);
                window.setAlwaysOnTop(true);
                window.setVisible(true);
                window.toFront();
                window.requestFocus();
            });
            robot.waitForIdle(); robot.delay(200);
            capture(robot, output, "home.png");
            check("home".equals(screen()), "home visible");
            click(robot, "newGame");
            check(state().getStatus() == GameState.Status.RUNNING, "new game button");
            press(robot, KeyEvent.VK_P);
            check(state().getStatus() == GameState.Status.PAUSED, "OS P pauses after start button focus");
            GameState paused = state();
            robot.delay(550);
            check(state().getTick() == paused.getTick(), "paused board does not tick");
            click(robot, "pause");
            check(state().getStatus() == GameState.Status.RUNNING, "resume button");
            int x = state().getPieceX();
            press(robot, KeyEvent.VK_LEFT);
            check(state().getPieceX() == x - 1, "OS left works after button click");
            press(robot, KeyEvent.VK_RIGHT);
            check(state().getPieceX() == x, "OS right");
            int y = state().getPieceY();
            press(robot, KeyEvent.VK_D);
            check(state().getPieceY() < y, "OS D soft drop");
            PieceType held = state().getActivePiece().getType();
            press(robot, KeyEvent.VK_C);
            check(state().getHoldPiece() == held && !state().canHold(), "OS C holds once");
            check(state().getNextPieces().size() == 3 && state().getGhostY() <= state().getPieceY(),
                    "NEXT and ghost snapshot");
            press(robot, KeyEvent.VK_SPACE);
            check(occupied(state()) >= 4, "OS Space hard drop");
            press(robot, KeyEvent.VK_ESCAPE);
            check("home".equals(screen()) && state().getStatus() == GameState.Status.PAUSED, "OS Escape suspends game");
            long homeTick = state().getTick();
            robot.delay(550);
            check(state().getTick() == homeTick, "no hidden gravity at home");
            click(robot, "continueGame");
            check("game".equals(screen()) && state().getStatus() == GameState.Status.RUNNING, "continue button");
            press(robot, KeyEvent.VK_P);
            SwingUtilities.invokeAndWait(() -> window.setSize(760, 680));
            robot.waitForIdle();
            capture(robot, output, "game.png");
            press(robot, KeyEvent.VK_P);
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < 100 && app.getState().getStatus() != GameState.Status.GAME_OVER; i++) {
                    app.submit(GameAction.Type.HARD_DROP);
                }
            });
            robot.waitForIdle();
            check("result".equals(screen()), "top-out result route");
            capture(robot, output, "result.png");
            click(robot, "retry");
            check(state().getLinesCleared() == 0 && "game".equals(screen()), "retry button creates new game");
            press(robot, KeyEvent.VK_ESCAPE);
            click(robot, "newBattle");
            check("battle".equals(screen()) && battle().getParticipants().size() == 2,
                    "monster battle button");
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (occupied(enemy()) == 0 && System.nanoTime() < deadline) {
                robot.delay(50); robot.waitForIdle();
            }
            check(occupied(enemy()) >= 4, "worker AI places through real battle engine");
            press(robot, KeyEvent.VK_C);
            check(state().getHoldPiece() != PieceType.EMPTY, "battle OS HOLD input");
            press(robot, KeyEvent.VK_P);
            check(battle().getStatus() == BattleState.Status.PAUSED, "battle OS pause");
            long enemyVersion = enemy().getVersion();
            robot.delay(800);
            check(enemy().getVersion() == enemyVersion && !app.isAiTimerRunning(),
                    "pause freezes monster and AI timer");
            capture(robot, output, "battle.png");
            click(robot, "battleHome");
            check("home".equals(screen()), "battle home button");
            long playerVersion = state().getVersion();
            robot.delay(550);
            check(state().getVersion() == playerVersion && enemy().getVersion() == enemyVersion,
                    "both boards frozen at home");
            click(robot, "continueGame");
            check("battle".equals(screen()) && battle().getStatus() == BattleState.Status.RUNNING,
                    "continue resumes battle");
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < 100 && app.getBattleState().getStatus() != BattleState.Status.FINISHED; i++) {
                    app.submit(GameAction.Type.HARD_DROP);
                }
            });
            robot.waitForIdle();
            check("result".equals(screen()) && !app.isAiTimerRunning(), "battle top-out result stops AI");
            capture(robot, output, "battle-result.png");
            click(robot, "retry");
            check("battle".equals(screen()) && battle().getParticipant("local").getHp() == 100,
                    "battle retry resets HP and keeps mode");
            SwingUtilities.invokeAndWait(() -> window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)));
            check(!window.isDisplayable() && !app.isGravityRunning() && !app.isAiTimerRunning(),
                    "window close stops session and AI timer");
            System.out.println("PASS DesktopSmoke: " + checks + " checks; screenshots: " + output.getAbsolutePath());
        } catch (Throwable failure) {
            if (window != null && window.isShowing()) capture(robot, output, "desktop-failure.png");
            throw failure;
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (app != null) app.close();
                if (window != null) window.dispose();
            });
        }
    }
    private static GameState state() throws Exception {
        AtomicReference<GameState> value = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> value.set(app.getState()));
        return value.get();
    }
    private static String screen() throws Exception {
        AtomicReference<String> value = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> value.set(app.getRouter().getCurrentId()));
        return value.get();
    }
    private static BattleState battle() throws Exception {
        AtomicReference<BattleState> value = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> value.set(app.getBattleState()));
        return value.get();
    }
    private static GameState enemy() throws Exception {
        return battle().getParticipant("monster").getGameState();
    }
    private static int occupied(GameState state) {
        int count = 0;
        for (int y = 0; y < state.getBoard().getHeight(); y++) {
            for (int x = 0; x < state.getBoard().getWidth(); x++) {
                if (state.getBoard().getCell(x, y) != PieceType.EMPTY) count++;
            }
        }
        return count;
    }
    private static void press(Robot robot, int key) throws Exception {
        requireFocus();
        robot.keyPress(key); robot.keyRelease(key); robot.waitForIdle();
    }
    private static void click(Robot robot, String name) throws Exception {
        AtomicReference<Point> point = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            if (!window.isFocused()) throw new AssertionError("Desktop test lost focus; refusing mouse input");
            JButton button = findButton(window, name);
            if (button == null) throw new AssertionError("Missing visible button: " + name);
            Point location = button.getLocationOnScreen();
            point.set(new Point(location.x + button.getWidth() / 2, location.y + button.getHeight() / 2));
        });
        robot.mouseMove(point.get().x, point.get().y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(150);
        robot.waitForIdle();
    }
    private static void requireFocus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (!window.isFocused()) throw new AssertionError("Desktop test lost focus; refusing keyboard input");
        });
    }
    private static JButton findButton(Container parent, String name) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton && name.equals(component.getName()) && component.isShowing()) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton found = findButton((Container) component, name);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void capture(Robot robot, File output, String name) throws Exception {
        AtomicReference<Rectangle> bounds = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> bounds.set(new Rectangle(window.getLocationOnScreen(), window.getSize())));
        ImageIO.write(robot.createScreenCapture(bounds.get()), "png", new File(output, name));
    }
}
