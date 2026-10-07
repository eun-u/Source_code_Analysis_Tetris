package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;

public class PlayerCard extends JPanel {

    private JLabel nicknameLabel;
    private JLabel levelLabel;
    private JLabel statusLabel;
    private JLabel characterLabel;

    // 데이터만 다르게 넣어 표시할 수 있는 Component
    public PlayerCard(PlayerData player) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(200, 180));
        setBorder(new LineBorder(Color.GRAY, 1));

        if (player == null) {
            nicknameLabel = new JLabel("상대를 기다리는 중...");
            levelLabel = new JLabel("");
            characterLabel = new JLabel("");
            statusLabel = new JLabel("WAITING...");
        }
        else {
            nicknameLabel = new JLabel(player.getNickname());
            levelLabel = new JLabel("Lv." + player.getLevel());
            characterLabel = new JLabel(player.getCharacterName());
            statusLabel = new JLabel(player.isReady() ? "READY" : "NOT READY");
        }

        nicknameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());

        add(nicknameLabel);
        add(Box.createVerticalStrut(5));

        add(levelLabel);
        add(Box.createVerticalStrut(20));

        add(characterLabel);
        add(Box.createVerticalStrut(10));

        add(statusLabel);
        add(Box.createVerticalGlue());


    }
}
