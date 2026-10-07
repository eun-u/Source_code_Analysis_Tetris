package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 성은 브랜치 CharacterView의 120×120 초상 자리와 아래 이름 구성 */
public final class CharacterView extends JPanel {
    private final JLabel portrait = new JLabel("Character", JLabel.CENTER);
    private final JLabel caption = new JLabel("");

    public CharacterView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Dimension imageSize = new Dimension(120, 120);
        portrait.setPreferredSize(imageSize);
        portrait.setMinimumSize(imageSize);
        portrait.setMaximumSize(imageSize);
        portrait.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        portrait.setAlignmentX(Component.CENTER_ALIGNMENT);
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(portrait);
        add(Box.createVerticalStrut(5));
        add(caption);
    }

    public void setCharacter(Icon icon, String text) {
        portrait.setIcon(icon);
        portrait.setText(icon == null ? "Character" : "");
        caption.setText(text);
    }
}
