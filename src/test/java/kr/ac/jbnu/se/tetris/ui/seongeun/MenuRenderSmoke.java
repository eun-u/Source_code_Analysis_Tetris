package kr.ac.jbnu.se.tetris.ui.seongeun;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.story.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.*;

/** 스토리 전 스테이지와 상점 화면 크기 확인용 PNG. */
public final class MenuRenderSmoke {
    private MenuRenderSmoke() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Output directory required");
        Path folder = Paths.get(args[0]);
        SwingUtilities.invokeAndWait(() -> {
            try {
                StageCatalog catalog = StageCatalog.loadDefault();
                StoryStageSelectPanel story = new StoryStageSelectPanel();
                story.updateProgress(new StoryProgressService(catalog).getCampaignProgress(), catalog);
                write(story, folder.resolve("story.png"));
                JTabbedPane chapters = tabs(story);
                chapters.setSelectedIndex(1); write(story, folder.resolve("story-graduation.png"));
                chapters.setSelectedIndex(2); write(story, folder.resolve("story-employment.png"));
                CharacterShopPanel shop = new CharacterShopPanel();
                Map<String,Integer> prices = new LinkedHashMap<String,Integer>();
                prices.put("student", 0); prices.put("attacker", 100);
                prices.put("defender", 100); prices.put("utility", 120);
                shop.setData(80, Collections.singleton("student"), "student", prices);
                write(shop, folder.resolve("shop.png"));
            } catch (Exception failure) { throw new RuntimeException(failure); }
        });
        System.out.println("PASS MenuRenderSmoke");
    }
    private static void write(JComponent component, Path path) throws Exception {
        kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme.apply(component);
        component.setSize(1020, 720);
        layout(component);
        BufferedImage image = new BufferedImage(1020, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }
    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container) layout((Container) child);
    }
    private static JTabbedPane tabs(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane) return (JTabbedPane) child;
            if (child instanceof Container) {
                JTabbedPane found = tabs((Container) child); if (found != null) return found;
            }
        }
        return null;
    }
}
