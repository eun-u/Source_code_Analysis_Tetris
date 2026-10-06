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
    private final JTextField roomId = new JTextField(38);

    public WaitingRoomPanel() {
        setLayout(new BorderLayout());

        titleLabel = new JLabel ("Waiting Room", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        setBackground(new Color(13, 23, 40));

        backButton = new GameButton("방 나가기");
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);
    
        roomId.setEditable(false);
        roomId.setToolTipText("다른 창의 '방 번호로 입장'에 이 번호를 입력하세요.");
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(topPanel, BorderLayout.NORTH);
        heading.add(roomId, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        // 참가자 카드
        playerPanel = new JPanel();
        playerPanel.setBackground(new Color(25, 41, 61));
        playerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 40));
        add(playerPanel, BorderLayout.CENTER);
        
        // 하단 Ready 버튼
        readyButton = new GameButton("READY");
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.add(readyButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setRoom(RoomData room) {
        titleLabel.setText(room.getRoomName());
    }
    public void setRoomId(String id) { roomId.setText(id == null ? "" : "방 번호: " + id); }
    // back 버튼 눌렀을 때 외부 결정
    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }
    // ready 버튼 눌렀을 때 외부 결정
    public void setReadyAction(ActionListener listener) {
        readyButton.addActionListener(listener);
    }

    public void setPlayers(PlayerData myPlayer, PlayerData enemyPlayer) {
        playerPanel.removeAll();

        PlayerCard myCard = new PlayerCard(myPlayer);
        PlayerCard enemyCard = new PlayerCard(enemyPlayer);

        playerPanel.add(enemyCard);
        playerPanel.add(myCard);
        

        playerPanel.revalidate();
        playerPanel.repaint();
    }
}
