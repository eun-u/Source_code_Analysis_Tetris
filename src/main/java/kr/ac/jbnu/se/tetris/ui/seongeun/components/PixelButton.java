package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

/** Swing 화면 전체에서 공통으로 사용하는 선명한 픽셀 버튼. */
public class PixelButton extends JButton {
    private Color accent = UniversityPixelTheme.GOLD;

    public PixelButton(String text) {
        super(text);
        setFont(UniversityPixelTheme.font(13, Font.BOLD));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setAccent(Color accent) {
        if (accent == null) throw new IllegalArgumentException("accent");
        this.accent = accent;
        super.setBackground(accent);
        repaint();
    }

    @Override public void setBackground(Color background) {
        super.setBackground(background);
        if (background != null && accent != null) accent = background;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int width = getWidth(), height = getHeight();
            if (width < 8 || height < 8) return;
            boolean enabled = isEnabled();
            Color base = enabled ? getModel().isPressed() ? accent.darker()
                    : getModel().isRollover() ? accent.brighter()
                    : accent : UniversityPixelTheme.PANEL_LIGHT;
            // CSS pixel-control의 검은 외곽선, 아래쪽 그림자, 밝은 내부 선.
            g.setColor(UniversityPixelTheme.BLACK);
            g.fillRect(3, 4, width - 3, height - 4);
            g.setColor(base);
            g.fillRect(3, 3, width - 7, height - 8);
            g.setColor(UniversityPixelTheme.BLACK);
            g.drawRect(1, 1, width - 5, height - 6);
            g.drawRect(2, 2, width - 7, height - 8);
            g.setColor(enabled ? UniversityPixelTheme.LINE : UniversityPixelTheme.BG);
            g.drawLine(5, 5, width - 9, 5);
            g.drawLine(5, 5, 5, height - 10);
            g.setFont(getFont());
            g.setColor(enabled ? UniversityPixelTheme.BLACK : UniversityPixelTheme.TEXT_SUB);
            FontMetrics metrics = g.getFontMetrics();
            String text = getText() == null ? "" : getText();
            int available = Math.max(0, width - 16);
            while (text.length() > 1 && metrics.stringWidth(text) > available) {
                text = text.substring(0, text.length() - 2) + "…";
            }
            int x = Math.max(7, (width - metrics.stringWidth(text)) / 2);
            int y = (height - metrics.getHeight()) / 2 + metrics.getAscent() - 2;
            g.drawString(text, x, y);
            if (isFocusOwner()) {
                g.setColor(UniversityPixelTheme.TEXT);
                g.drawRect(6, 6, width - 14, height - 16);
            }
        } finally { g.dispose(); }
    }
}
