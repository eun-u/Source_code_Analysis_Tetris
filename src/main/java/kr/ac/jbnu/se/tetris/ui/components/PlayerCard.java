package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/** 성은 브랜치 PlayerCard의 200×180 참가자 카드와 네 줄 구성 */
public final class PlayerCard extends JPanel {
    private final JLabel nickname = new JLabel("");
    private final JLabel level = new JLabel("");
    private final JLabel character = new JLabel("");
    private final JLabel status = new JLabel("");

    public PlayerCard() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(200, 180));
        setBorder(new LineBorder(Color.GRAY, 1));
        for (JLabel label : new JLabel[] {nickname, level, character, status}) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        add(Box.createVerticalGlue());
        add(nickname);
        add(Box.createVerticalStrut(5));
        add(level);
        add(Box.createVerticalStrut(20));
        add(character);
        add(Box.createVerticalStrut(10));
        add(status);
        add(Box.createVerticalGlue());
    }

    public void setParticipant(String id, boolean local, boolean ready) {
        nickname.setText(local ? "Player" : "Enemy");
        nickname.setToolTipText(id);
        level.setText("");
        character.setText("");
        status.setText(ready ? "READY" : "NOT READY");
    }

    public void setEmpty() {
        nickname.setText("상대를 기다리는 중...");
        level.setText("");
        character.setText("");
        status.setText("WAITING...");
    }
}
