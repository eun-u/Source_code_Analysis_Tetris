package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 메뉴 화면 공통 배경. 한국 캠퍼스 풍경과 작게 흘러내리는 콘크리트 조각을 그린다. */
public class ScenePanel extends JPanel {
    private static final BufferedImage CAMPUS = readCampus();
    private final Timer timer = new Timer(50, event -> repaint());
    private boolean animated = true;

    public ScenePanel() { this(null); }

    public ScenePanel(LayoutManager layout) {
        super(layout == null ? new FlowLayout() : layout);
        setBackground(UniversityPixelTheme.BG);
        timer.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing() && animated) timer.start(); else timer.stop();
            }
        });
    }

    /** 정적인 화면이 필요한 경우 배경 움직임을 끈다. */
    public void setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) timer.stop(); else if (isShowing()) timer.start();
    }

    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try { paintBackdrop(g, getWidth(), getHeight(), animated ? System.currentTimeMillis() : 0); }
        finally { g.dispose(); }
    }

    public static void paintBackdrop(Graphics2D g, int w, int h, long now) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x16272E), 0, h, UniversityPixelTheme.BG));
        g.fillRect(0, 0, w, h);
        if (CAMPUS != null && w > 0 && h > 0) {
            int width = Math.max(w, CAMPUS.getWidth() * h / CAMPUS.getHeight());
            g.drawImage(CAMPUS, (w - width) / 2, 0, width, h, null);
        }
        g.setColor(new Color(8, 17, 21, 178));
        g.fillRect(0, 0, w, h);
        g.setPaint(new GradientPaint(0, 0, new Color(7, 15, 19, 115), w, 0,
                new Color(7, 15, 19, 28)));
        g.fillRect(0, 0, w, h);
        int cell = Math.max(8, Math.min(15, w / 80));
        for (int i = 0; i < 8; i++) {
            long cycle = 17000 + i * 1900L;
            float t = ((now + i * 3711L) % cycle) / (float) cycle;
            int x = Math.floorMod(i * 211 + 40, Math.max(1, w - cell));
            int y = Math.round(-cell + (h + cell * 2) * t);
            g.setColor(new Color(191, 194, 182, 31));
            g.fillRect(x, y, cell, cell);
            g.setColor(new Color(235, 195, 112, 43));
            g.drawLine(x, y, x + cell - 1, y);
        }
    }

    private static BufferedImage readCampus() {
        try {
            java.net.URL resource = ScenePanel.class.getResource("/ui/campus-rpg/university-bg.png");
            return resource == null ? null : ImageIO.read(resource);
        } catch (IOException unavailable) { return null; }
    }
}
