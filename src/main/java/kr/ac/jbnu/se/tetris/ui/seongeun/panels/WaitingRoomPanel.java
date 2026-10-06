package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PlayerCard;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

public class WaitingRoomPanel extends JPanel {
    
    private GameButton backButton;
    private GameButton readyButton;
    private JLabel titleLabel;
    private JPanel playerPanel;

    public WaitingRoomPanel() {
        setLayout(new BorderLayout());

        titleLabel = new JLabel ("Waiting Room", SwingConstants.CENTER);

        backButton = new GameButton("방 나가기");
        
        JPanel topPanel = new JPanel(new BorderLayout());

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);
    
        add(topPanel, BorderLayout.NORTH);

        // 참가자 카드
        playerPanel = new JPanel();
        playerPanel.setBackground(Color.LIGHT_GRAY);
        playerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 40));
        add(playerPanel, BorderLayout.CENTER);
        
        // 하단 Ready 버튼
        readyButton = new GameButton("READY");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(readyButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setRoom(RoomData room) {
        titleLabel.setText(room.getRoomName());
    }
    // back 버튼 눌렀을 때 외부 결정
    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }
    // ready 버튼 눌렀을 때 외부 결정
    public void setReadyAction(ActionListener listener) {
        readyButton.addActionListener(listener);
    }

    public void setPlayers(PlayerData myPlayer, PlayerData enemyPlayer) {
        System.out.println("setPlayers 실행됨");
        playerPanel.removeAll();

        PlayerCard myCard = new PlayerCard(myPlayer);
        PlayerCard enemyCard = new PlayerCard(enemyPlayer);

        playerPanel.add(enemyCard);
        playerPanel.add(myCard);
        

        playerPanel.revalidate();
        playerPanel.repaint();
    }
}
