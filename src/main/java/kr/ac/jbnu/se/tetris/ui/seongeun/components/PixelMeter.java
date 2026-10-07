package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

/** 검은 테두리 안을 칸 단위로 채우는 픽셀 게이지. 진행도·피버처럼 0..max 값을 보여 준다. */
public final class PixelMeter extends JComponent {
    private int value, maximum;
    private Color fill;

    public PixelMeter(int maximum, Color fill) {
        this.maximum = Math.max(1, maximum);
        this.fill = fill;
        setPreferredSize(new Dimension(160, 14));
        setMinimumSize(new Dimension(40, 10));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
    }

    public void setValue(int value) {
        int clamped = Math.max(0, Math.min(maximum, value));
        if (clamped != this.value) { this.value = clamped; repaint(); }
    }

    public int getValue() { return value; }

    public void setMaximum(int maximum) { this.maximum = Math.max(1, maximum); repaint(); }

    public void setFill(Color fill) { this.fill = fill; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int width = getWidth(), height = getHeight();
            g.setColor(UniversityPixelTheme.BLACK);
            g.fillRect(0, 0, width, height);
            int inner = width - 4;
            int filled = (int) Math.round(inner * (value / (double) maximum));
            g.setColor(UniversityPixelTheme.PANEL_LIGHT);
            g.fillRect(2, 2, inner, height - 4);
            g.setColor(fill);
            g.fillRect(2, 2, filled, height - 4);
            // 위쪽 밝은 줄과 4px 마다 끊긴 눈금으로 픽셀 게이지처럼 보이게 한다.
            g.setColor(new Color(255, 255, 255, 70));
            g.fillRect(2, 2, filled, Math.max(1, (height - 4) / 3));
            g.setColor(UniversityPixelTheme.BLACK);
            for (int x = 2 + 8; x < 2 + inner; x += 8) g.fillRect(x, 2, 1, height - 4);
        } finally { g.dispose(); }
    }
}
