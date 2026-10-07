package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import javax.swing.*;

/**
 * 메뉴 화면 공통 배경. 짙은 밤하늘, 깜박이는 별, 천천히 떨어지는 반투명 테트로미노,
 * 아래쪽 원근 격자를 그린다. 화면이 보일 때만 타이머가 돈다.
 */
public class ScenePanel extends JPanel {
    private static final int[][][] SHAPES = {
        {{0, 0}, {1, 0}, {2, 0}, {3, 0}}, {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
        {{0, 0}, {1, 0}, {2, 0}, {1, 1}}, {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
        {{0, 0}, {1, 0}, {1, 1}, {2, 1}}, {{0, 0}, {0, 1}, {1, 1}, {2, 1}},
        {{2, 0}, {0, 1}, {1, 1}, {2, 1}}
    };
    private static final Color[] COLORS = {
        new Color(104, 221, 235), new Color(255, 221, 115), new Color(201, 139, 255),
        new Color(112, 217, 155), new Color(255, 139, 113), new Color(114, 157, 255), new Color(253, 183, 97)
    };
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
        g.setPaint(new GradientPaint(0, 0, new Color(0x07071F), 0, h, new Color(0x1A1350)));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 60; i++) {
            int x = Math.floorMod(i * 137 + 29, Math.max(1, w));
            int y = Math.floorMod(i * 89 + 11, Math.max(1, h * 2 / 3));
            boolean bright = (now / 300 + i) % 7 == 0;
            g.setColor(new Color(255, 247, 232, bright ? 200 : 55 + (i % 3) * 20));
            int size = bright ? 3 : i % 4 == 0 ? 2 : 1;
            g.fillRect(x, y, size, size);
        }
        // 떨어지는 블록: 열마다 속도와 모양이 다르고 아주 옅게 그린다.
        int cell = Math.max(10, Math.min(18, w / 60));
        for (int i = 0; i < 9; i++) {
            long cycle = 14000 + i * 2300L;
            float t = ((now + i * 3911L) % cycle) / (float) cycle;
            int x = Math.floorMod(i * 211 + 40, Math.max(1, w - cell * 4));
            int y = Math.round(-cell * 4 + (h + cell * 8) * t);
            int[][] shape = SHAPES[i % SHAPES.length];
            Color color = COLORS[i % COLORS.length];
            for (int[] block : shape) {
                int bx = x + block[0] * cell, by = y + block[1] * cell;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 34));
                g.fillRect(bx, by, cell - 1, cell - 1);
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g.drawRect(bx, by, cell - 2, cell - 2);
            }
        }
        int horizon = h * 3 / 4;
        g.setColor(new Color(81, 72, 166, 50));
        for (int i = -10; i <= 10; i++) g.drawLine(w / 2 + i * 30, horizon, w / 2 + i * w / 4, h);
        float scroll = (now % 3000) / 3000f;
        for (int i = 0; i < 6; i++) {
            float t = (i + scroll) / 6f;
            int y = horizon + Math.round((h - horizon) * t * t);
            g.drawLine(0, y, w, y);
        }
        g.setColor(new Color(81, 72, 166, 110));
        g.fillRect(0, horizon, w, 1);
    }
}
