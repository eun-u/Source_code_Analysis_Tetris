package kr.ac.jbnu.se.tetris.ui.seongeun;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import kr.ac.jbnu.se.tetris.battle.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.BattlePanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** headless 구성의 실제 전투 스냅샷을 PNG로 확인하는 독립 렌더 도구. */
public final class BattlePanelRenderSmoke {
    private BattlePanelRenderSmoke() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3)
            throw new IllegalArgumentException("Output PNG path [width height] required");
        Path output = Paths.get(args[0]);
        final int width = args.length == 3 ? Integer.parseInt(args[1]) : 1160;
        final int height = args.length == 3 ? Integer.parseInt(args[2]) : 780;
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattleManager manager = BattleManager.pve(Arrays.asList(
                        new ParticipantSpec("local", "플레이어", 100),
                        new ParticipantSpec("monster", "슬라임 군주", 150)), 17L, 500, 500, 0);
                manager.start();
                BattlePanel panel = new BattlePanel();
                panel.setState(manager.getState(), "local");
                JScrollPane viewport = new JScrollPane(panel);
                viewport.setBorder(null);
                UniversityPixelTheme.apply(viewport);
                viewport.setSize(width, height);
                layout(viewport);
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                viewport.printAll(graphics);
                graphics.dispose();
                ImageIO.write(image, "png", output.toFile());
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        System.out.println("PASS BattlePanelRenderSmoke " + output);
    }
    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container) layout((Container) child);
    }
}
