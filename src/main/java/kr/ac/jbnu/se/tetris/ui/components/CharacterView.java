package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 성은 브랜치 CharacterView의 초상 영역 구성 이식 */
public final class CharacterView extends JPanel {
    private final JLabel portrait = new JLabel("", JLabel.CENTER);
    private final JLabel caption = new JLabel("", JLabel.CENTER);

    public CharacterView() {
        super(new BorderLayout(0, 3));
        portrait.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        portrait.setPreferredSize(new Dimension(54, 54));
        add(portrait, BorderLayout.CENTER);
        add(caption, BorderLayout.SOUTH);
    }

    public void setCharacter(Icon icon, String text) {
        portrait.setIcon(icon);
        portrait.setText(icon == null ? "?" : "");
        caption.setText(text);
    }
}
