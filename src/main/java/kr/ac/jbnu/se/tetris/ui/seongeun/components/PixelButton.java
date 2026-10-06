package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

/** Swing 화면 전체에서 공통으로 사용하는 선명한 픽셀 버튼. */
public class PixelButton extends JButton {
    public PixelButton(String text) {
        super(text);
        setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            boolean enabled = isEnabled();
            Color base = enabled ? getModel().isPressed() ? new Color(49, 89, 112)
                    : getModel().isRollover() ? new Color(45, 74, 101)
                    : new Color(32, 54, 78) : new Color(28, 41, 58);
            g.setColor(base);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(enabled ? new Color(111, 170, 191) : new Color(67, 87, 105));
            g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            g.setFont(getFont());
            g.setColor(enabled ? new Color(237, 245, 246) : new Color(128, 151, 164));
            FontMetrics metrics = g.getFontMetrics();
            String text = getText() == null ? "" : getText();
            int x = Math.max(4, (getWidth() - metrics.stringWidth(text)) / 2);
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(text, x, y);
            if (isFocusOwner()) {
                g.setColor(new Color(255, 225, 135));
                g.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
            }
        } finally { g.dispose(); }
    }
}
