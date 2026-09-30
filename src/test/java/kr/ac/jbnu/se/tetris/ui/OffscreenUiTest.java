package kr.ac.jbnu.se.tetris.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import kr.ac.jbnu.se.tetris.app.TetrisApplication;
import kr.ac.jbnu.se.tetris.core.GameAction;

/** 사용자 데스크톱 조작 없는 최소·기본 크기 화면의 이미지 버퍼 렌더링 */
public final class OffscreenUiTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TetrisApplication app = new TetrisApplication();
            try {
                capture(app, "home");
                app.startNewGame(42); capture(app, "tutorial");
                for (GameAction.Type action : new GameAction.Type[] {GameAction.Type.MOVE_LEFT,
                        GameAction.Type.ROTATE_RIGHT, GameAction.Type.SOFT_DROP,
                        GameAction.Type.HOLD, GameAction.Type.HARD_DROP}) app.submit(action);
                capture(app, "tutorial-result");
                app.showStages(); capture(app, "stage-select");
                app.startStory(0, 7); capture(app, "story-battle");
                app.showHome(); capture(app, "paused-home");
                app.showOnline(); capture(app, "online-lobby");
                app.showAccount(); capture(app, "online-account");
            } finally { app.close(); }
            JPanel preview = UiPreviewMain.createPanel();
            JTabbedPane tabs = null;
            for (Component child : preview.getComponents()) {
                if (child instanceof JTabbedPane) tabs = (JTabbedPane) child;
            }
            if (tabs == null) throw new AssertionError("Missing preview tabs");
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setSelectedIndex(i);
                capture(preview, "preview-" + i);
            }
        });
        System.out.println("PASS OffscreenUiTest: 8 app screens + 8 preview tabs at 2 sizes, no native window");
    }
    private static void capture(TetrisApplication app, String name) {
        capture(app.getRouter().getContainer(), name);
    }
    private static void capture(Container root, String name) {
        for (int[] size : new int[][] {{760, 680}, {960, 820}}) {
            root.setSize(size[0], size[1]); layout(root);
            BufferedImage buffer = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = buffer.createGraphics();
            try { root.paint(graphics); } finally { graphics.dispose(); }
            File target = new File("out/g0/ui/" + name + "-" + size[0] + ".png");
            if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                throw new IllegalStateException("Cannot create offscreen output directory");
            }
            try { ImageIO.write(buffer, "png", target); }
            catch (IOException failure) { throw new IllegalStateException("Cannot write rendered screen", failure); }
        }
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child.isVisible() && child instanceof Container) layout((Container) child);
        }
    }
}
