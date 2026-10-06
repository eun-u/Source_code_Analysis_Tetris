package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.RoomRow;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

public class RoomListPanel extends JPanel {

    private GameButton backButton;
    private GameButton createRoomButton;
    private JPanel roomContainer;
    private final GameButton joinByIdButton = new GameButton("방 번호로 입장");
    private final JLabel connectionLabel = new JLabel("서버 연결 중...");

    public RoomListPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(13, 23, 40));

        JLabel titleLabel = new JLabel("Online Battle - Room List", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));

        createRoomButton = new GameButton("방 만들기");
        backButton = new GameButton("Back");

        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setOpaque(false);
        topButtonPanel.add(createRoomButton);
        topButtonPanel.add(joinByIdButton);
        topButtonPanel.add(backButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(topButtonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        roomContainer = new JPanel();
        roomContainer.setBackground(new Color(13, 23, 40));
        roomContainer.setLayout(new BoxLayout(roomContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(roomContainer);

        JPanel listArea = new JPanel(new BorderLayout());
        connectionLabel.setForeground(new Color(169, 205, 218));
        listArea.add(connectionLabel, BorderLayout.NORTH);
        listArea.add(scrollPane, BorderLayout.CENTER);

        add(listArea, BorderLayout.CENTER);
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void setCreateRoomAction(ActionListener listener) {
        createRoomButton.addActionListener(listener);
    }
    public void setJoinByIdAction(ActionListener listener) { joinByIdButton.addActionListener(listener); }
    public void setConnected(boolean connected) {
        connectionLabel.setText(connected ? "서버 연결됨 · 방 번호를 입력하거나 방을 만드세요."
                : "서버 연결 중 · 로컬 서버 시작은 로비에서 가능합니다.");
    }

    public void setRooms(List<RoomData> rooms, Consumer<RoomData> joinAction) {
        roomContainer.removeAll();
        if (rooms.isEmpty()) {
            JLabel hint = new JLabel("방 목록 조회를 지원하지 않습니다. 초대받은 방 번호로 입장하세요.");
            hint.setForeground(new Color(179, 208, 217));
            roomContainer.add(hint);
        }

        for (RoomData room : rooms) {
            RoomRow row = new RoomRow(room);
            row.setJoinAction(joinAction);
            roomContainer.add(row);
        }

        roomContainer.revalidate();
        roomContainer.repaint();
    }
}
