package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

/** Swing 화면 전체에서 공통으로 사용하는 절제된 픽셀 버튼. */
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

    /** 화면의 주된 다음 행동. 한 화면에 하나만 두는 것을 기본으로 한다. */
    public PixelButton primary() { setAccent(UniversityPixelTheme.GOLD); return this; }

    /** 돌아가기·취소처럼 흐름을 바꾸지 않는 보조 행동. */
    public PixelButton secondary() { setAccent(UniversityPixelTheme.PANEL_LIGHT); return this; }

    /** 대전 시작·준비처럼 긍정적인 확정 행동. */
    public PixelButton positive() { setAccent(UniversityPixelTheme.MINT); return this; }

    /** 대전 포기·방 나가기처럼 진행 중인 상태를 끊는 행동. */
    public PixelButton danger() { setAccent(UniversityPixelTheme.CORAL); return this; }

    @Override public void setBackground(Color background) {
        super.setBackground(background);
        if (background != null && accent != null) accent = background;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int width = getWidth(), height = getHeight();
            if (width < 8 || height < 8) return;
            boolean enabled = isEnabled();
            boolean pressed = enabled && getModel().isPressed();
            boolean hover = enabled && getModel().isRollover();
            // 눌린 버튼은 그림자 쪽으로 한 칸 내려가 실제로 눌린 것처럼 보인다.
            int offset = pressed ? 2 : 0;
            int faceWidth = width - 4, faceHeight = height - 5;
            if (enabled) {
                g.setColor(UniversityPixelTheme.LINE);
                g.fillRect(3, 4, width - 3, height - 4);
            }
            Color base = !enabled ? UniversityPixelTheme.PANEL
                    : pressed ? accent.darker() : hover ? lighten(accent) : accent;
            g.setColor(base);
            g.fillRect(1 + offset, 1 + offset, faceWidth - 1, faceHeight - 1);
            g.setColor(enabled ? UniversityPixelTheme.TEXT_SUB : UniversityPixelTheme.LINE);
            g.drawRect(offset, offset, faceWidth, faceHeight);
            g.setColor(enabled ? UniversityPixelTheme.LINE : UniversityPixelTheme.PANEL);
            g.drawRect(1 + offset, 1 + offset, faceWidth - 2, faceHeight - 2);
            if (enabled) {
                // 위·왼쪽은 밝게, 아래·오른쪽은 어둡게 칠한 픽셀 베벨.
                g.setColor(lighten(base));
                g.drawLine(3 + offset, 3 + offset, faceWidth - 3 + offset, 3 + offset);
                g.drawLine(3 + offset, 3 + offset, 3 + offset, faceHeight - 3 + offset);
                g.setColor(base.darker());
                g.drawLine(3 + offset, faceHeight - 2 + offset, faceWidth - 2 + offset, faceHeight - 2 + offset);
                g.drawLine(faceWidth - 2 + offset, 3 + offset, faceWidth - 2 + offset, faceHeight - 2 + offset);
            }
            g.setFont(getFont());
            g.setColor(enabled ? UniversityPixelTheme.readableOn(base) : UniversityPixelTheme.TEXT_MUTED);
            FontMetrics metrics = g.getFontMetrics();
            String text = getText() == null ? "" : getText();
            int available = Math.max(0, faceWidth - 14);
            while (text.length() > 1 && metrics.stringWidth(text) > available) {
                text = text.substring(0, text.length() - 2) + "…";
            }
            int x = Math.max(7, (faceWidth - metrics.stringWidth(text)) / 2 + 1) + offset;
            int y = (faceHeight - metrics.getHeight()) / 2 + metrics.getAscent() + 1 + offset;
            g.drawString(text, x, y);
            if (isFocusOwner()) {
                g.setColor(UniversityPixelTheme.readableOn(base));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        1f, new float[] { 2f, 2f }, 0f));
                g.drawRect(5 + offset, 5 + offset, faceWidth - 10, faceHeight - 10);
            }
        } finally { g.dispose(); }
    }

    private static Color lighten(Color color) {
        return new Color(Math.min(255, color.getRed() + 28), Math.min(255, color.getGreen() + 28),
                Math.min(255, color.getBlue() + 28));
    }
}
