package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** University_Simulation의 임시 픽셀 UI 토큰을 Swing에 옮긴 공통 테마. */
public final class UniversityPixelTheme {
    public static final Color BG = new Color(0x0B0B2E);
    public static final Color PANEL = new Color(0x1E174D);
    public static final Color PANEL_LIGHT = new Color(0x2B2166);
    public static final Color TEXT = new Color(0xFFF7E8);
    public static final Color TEXT_SUB = new Color(0xC9C1F2);
    public static final Color GOLD = new Color(0xFFD166);
    public static final Color MINT = new Color(0x4EE39A);
    public static final Color CORAL = new Color(0xFF5C7A);
    public static final Color LINE = new Color(0x5148A6);
    public static final Color BLACK = new Color(0x03030D);
    private static final String FONT_FAMILY = chooseFont();

    private UniversityPixelTheme() { }

    public static Font font(int size, int style) {
        return new Font(FONT_FAMILY, style, size);
    }

    /** 현재 화면과 자식 위젯에 적용한다. 기존 레이아웃과 사용자 입력 설정은 건드리지 않는다. */
    public static void apply(Component component) {
        if (component == null) return;
        if (component instanceof JPanel) component.setBackground(BG);
        if (component instanceof JScrollPane) component.setBackground(BG);
        if (component instanceof JTextField) component.setBackground(PANEL);
        if (component instanceof JLabel || component instanceof AbstractButton
                || component instanceof JTextField) {
            component.setForeground(TEXT);
            Font previous = component.getFont();
            if (previous != null) component.setFont(font(previous.getSize(), previous.getStyle()));
        }
        if (component instanceof AbstractButton && !(component instanceof PixelButton)) {
            AbstractButton button = (AbstractButton) component;
            button.setBackground(PANEL_LIGHT);
            button.setForeground(TEXT);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BLACK, 3),
                    BorderFactory.createEmptyBorder(3, 7, 3, 7)));
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) apply(child);
        }
    }

    private static String chooseFont() {
        String[] preferred = { "NeoDunggeunmo", "DungGeunMo", "Galmuri11", "Malgun Gothic" };
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : preferred) {
            for (String name : installed) {
                if (candidate.equalsIgnoreCase(name)) return name;
            }
        }
        return Font.DIALOG;
    }
}
