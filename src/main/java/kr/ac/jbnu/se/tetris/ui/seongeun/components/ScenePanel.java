package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.AlphaComposite;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 메뉴 화면 공통 배경. 캠퍼스 풍경을 밝은 콘크리트 면 아래에 은은하게 남긴다. */
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
        g.setPaint(new GradientPaint(0, 0, new Color(0xD8DDD5), 0, h, UniversityPixelTheme.BG));
        g.fillRect(0, 0, w, h);
        if (CAMPUS != null && w > 0 && h > 0) {
            int width = Math.max(w, CAMPUS.getWidth() * h / CAMPUS.getHeight());
            g.setComposite(AlphaComposite.SrcOver.derive(0.27f));
            g.drawImage(CAMPUS, (w - width) / 2, 0, width, h, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
        // 제목과 메뉴가 놓이는 상단은 같은 밝기로 묶어 읽기 쉽게 한다.
        g.setPaint(new GradientPaint(0, 0, new Color(235, 234, 222, 54), 0, h,
                new Color(211, 214, 203, 54)));
        g.fillRect(0, 0, w, h);
        int cell = Math.max(8, Math.min(15, w / 80));
        for (int i = 0; i < 4; i++) {
            long cycle = 17000 + i * 1900L;
            float t = ((now + i * 3711L) % cycle) / (float) cycle;
            int x = Math.floorMod(i * 211 + 40, Math.max(1, w - cell));
            int y = Math.round(-cell + (h + cell * 2) * t);
            g.setColor(new Color(66, 80, 80, 12));
            g.fillRect(x, y, cell, cell);
            g.setColor(new Color(150, 101, 31, 21));
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
