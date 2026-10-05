package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.*;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.model.PlayerData;

public class CharacterView extends JPanel {
    private JLabel characterImageLabel; // 캐릭터 이미지 표시
    private JLabel characterNameLabel; // 캐릭터 이름 표시

    public CharacterView(PlayerData player) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // 이미지 영역 만들기
        characterImageLabel = new JLabel("Character", SwingConstants.CENTER); 
        characterImageLabel.setPreferredSize(new Dimension(120, 120));
        characterImageLabel.setMinimumSize(new Dimension(120, 120));
        characterImageLabel.setMaximumSize(new Dimension(120, 120));
        characterImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 테두리 감싸기

        // 캐릭터 이름 표시
        characterNameLabel = new JLabel(player.getCharacterName());

        // 가운데 정렬
        characterImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 화면 추가
        add(characterImageLabel);
        add(Box.createVerticalStrut(5));
        add(characterNameLabel);

    }

}