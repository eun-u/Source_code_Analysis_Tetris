package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PlayerCard;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

public class WaitingRoomPanel extends JPanel {
    
    private GameButton backButton;
    private GameButton readyButton;
    private JLabel titleLabel;
    private JPanel playerPanel;
    private final JTextField roomId = new JTextField(38);

    public WaitingRoomPanel() {
        setLayout(new BorderLayout(0, 15));
        setBorder(new EmptyBorder(18, 22, 18, 22));

        titleLabel = new JLabel ("대기실");
        titleLabel.setForeground(UniversityPixelTheme.GOLD);
        titleLabel.setFont(UniversityPixelTheme.font(23, Font.BOLD));
        setBackground(UniversityPixelTheme.BG);

        backButton = new GameButton("방 나가기");
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);
    
        roomId.setEditable(false);
        roomId.setText("초대할 상대에게 방 번호를 알려주세요.");
        roomId.setToolTipText("다른 창의 '방 번호로 입장'에 이 번호를 입력하세요.");
        roomId.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        roomId.setForeground(UniversityPixelTheme.TEXT);
        roomId.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        roomId.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(8, 11, 8, 11)));
        JPanel heading = new JPanel(new BorderLayout(0, 12));
        heading.setOpaque(false);
        heading.add(topPanel, BorderLayout.NORTH);
        heading.add(roomId, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        // 참가자 카드
        playerPanel = new JPanel();
        playerPanel.setBackground(UniversityPixelTheme.PANEL);
        playerPanel.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3));
        playerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 24, 30));
        JLabel waiting = new JLabel("상대가 입장하면 여기서 준비 상태를 확인할 수 있습니다.");
        waiting.setForeground(UniversityPixelTheme.TEXT_SUB);
        waiting.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        playerPanel.add(waiting);
        add(playerPanel, BorderLayout.CENTER);
        
        // 하단 Ready 버튼
        readyButton = new GameButton("준비 완료");
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.add(readyButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setRoom(RoomData room) {
        titleLabel.setText(room.getRoomName());
    }
    public void setRoomId(String id) {
        roomId.setText(id == null || id.isEmpty() ? "초대할 상대에게 방 번호를 알려주세요." : "방 번호: " + id);
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
        playerPanel.removeAll();

        PlayerCard myCard = new PlayerCard(myPlayer);
        PlayerCard enemyCard = new PlayerCard(enemyPlayer);
        styleCard(myCard, UniversityPixelTheme.MINT);
        styleCard(enemyCard, UniversityPixelTheme.CORAL);

        playerPanel.add(enemyCard);
        playerPanel.add(myCard);
        

        playerPanel.revalidate();
        playerPanel.repaint();
    }
    private void styleCard(PlayerCard card, Color accent) {
        card.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        card.setBorder(BorderFactory.createLineBorder(accent, 2));
        for (Component child : card.getComponents()) {
            if (child instanceof JLabel) {
                child.setForeground(UniversityPixelTheme.TEXT);
                child.setFont(UniversityPixelTheme.font(14, Font.BOLD));
            }
        }
    }
}
