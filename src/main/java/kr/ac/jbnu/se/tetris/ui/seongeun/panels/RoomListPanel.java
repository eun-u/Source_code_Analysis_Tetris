package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.RoomRow;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

public class RoomListPanel extends JPanel {

    private GameButton backButton;
    private GameButton createRoomButton;
    private JPanel roomContainer;
    private final GameButton joinByIdButton = new GameButton("방 번호로 입장");
    private final JLabel connectionLabel = new JLabel("서버 연결 중...");

    public RoomListPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel titleLabel = new JLabel("BATTLE LOBBY  /  방 목록");
        titleLabel.setForeground(UniversityPixelTheme.GOLD);
        titleLabel.setFont(UniversityPixelTheme.font(23, Font.BOLD));

        createRoomButton = new GameButton("방 만들기");
        backButton = new GameButton("로비로");

        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
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
        roomContainer.setBackground(UniversityPixelTheme.BG);
        roomContainer.setLayout(new BoxLayout(roomContainer, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(roomContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2));
        scrollPane.getViewport().setBackground(UniversityPixelTheme.BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel listArea = new JPanel(new BorderLayout(0, 12));
        listArea.setOpaque(false);
        connectionLabel.setForeground(UniversityPixelTheme.TEXT_SUB);
        connectionLabel.setFont(UniversityPixelTheme.font(13, Font.BOLD));
        connectionLabel.setBorder(new EmptyBorder(5, 8, 0, 0));
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
            JLabel hint = new JLabel("방 번호를 입력하거나 새 방을 만들어 전투하세요.", SwingConstants.CENTER);
            hint.setFont(UniversityPixelTheme.font(15, Font.BOLD));
            hint.setBorder(new EmptyBorder(30, 8, 30, 8));
            hint.setForeground(UniversityPixelTheme.TEXT_SUB);
            roomContainer.add(hint);
        }

        for (RoomData room : rooms) {
            RoomRow row = new RoomRow(room);
            row.setBackground(UniversityPixelTheme.PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, UniversityPixelTheme.LINE),
                    new EmptyBorder(8, 12, 8, 12)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            for (Component child : row.getComponents()) {
                if (child instanceof JLabel) {
                    child.setForeground(UniversityPixelTheme.TEXT);
                    child.setFont(UniversityPixelTheme.font(14, Font.BOLD));
                }
            }
            row.setJoinAction(joinAction);
            roomContainer.add(row);
        }

        roomContainer.revalidate();
        roomContainer.repaint();
    }
}
