package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.Dimension;
import javax.swing.JButton;

/** 성은 브랜치 GameButton의 기본 버튼 크기 이식 */
public final class GameButton extends JButton {
    public GameButton(String text) {
        super(text);
        setPreferredSize(new Dimension(120, 35));
        setFocusPainted(false);
    }
}
