package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

/** 얇은 콘크리트 테두리 안을 칸 단위로 채우는 픽셀 게이지. */
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
            int w = getWidth(), h = getHeight();
            if (w < 10 || h < 10) return;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double ratio = Math.max(0.0, Math.min(1.0, value / (double) Math.max(1, maximum)));

            // 바깥쪽 검은 캡슐과 밝은 미세 테두리
            int arc = Math.min(h, 24);
            g.setColor(new Color(0x151B1A));
            g.fillRoundRect(0, 0, w, h, arc, arc);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(0x6E8078));
            g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            // 게이지 트랙, 진행률에 따라 채워지는 밝은 연두색
            int x = 4, y = 4, barW = w - 8, barH = h - 8;
            int barArc = Math.min(barH, 18);
            g.setColor(new Color(0x30383B));
            g.fillRoundRect(x, y, barW, barH, barArc, barArc);
            java.awt.Shape oldClip = g.getClip();
            g.clip(new java.awt.geom.RoundRectangle2D.Float(x, y, barW, barH, barArc, barArc));
            int filled = (int) Math.round(barW * ratio);
            if (filled > 0) {
                g.setColor(fill);
                g.fillRect(x, y, filled, barH);
                g.setColor(new Color(255, 255, 255, 85));
                g.fillRect(x, y + 1, filled, Math.max(2, barH / 3));
            }
            g.setColor(new Color(0x1A2828));
            for (int tick = x + 16; tick < x + barW - 1; tick += 16) {
                if (tick >= x + filled) g.drawLine(tick, y + 2, tick, y + barH - 3);
            }
            g.setClip(oldClip);
        } finally {
            g.dispose();
        }
    }
}
