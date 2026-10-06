package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 성은 브랜치 PlayerCard의 참가자 카드 구성 이식 */
public final class PlayerCard extends JPanel {
    private final JLabel nickname = new JLabel("", JLabel.CENTER);
    private final JLabel status = new JLabel("", JLabel.CENTER);

    public PlayerCard() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(180, 110));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        nickname.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(Box.createVerticalGlue());
        add(nickname);
        add(Box.createVerticalStrut(12));
        add(status);
        add(Box.createVerticalGlue());
    }

    public void setParticipant(String id, boolean local, boolean ready) {
        nickname.setText(id + (local ? " · 나" : ""));
        status.setText(ready ? "준비 완료" : "대기 중");
    }

    public void setEmpty() {
        nickname.setText("상대를 기다리는 중...");
        status.setText("빈 자리");
    }
}
