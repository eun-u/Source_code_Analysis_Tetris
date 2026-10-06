package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.BorderLayout;
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

    public RoomListPanel() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Online Battle - Room List", SwingConstants.CENTER);

        createRoomButton = new GameButton("방 만들기");
        backButton = new GameButton("Back");

        JPanel topButtonPanel = new JPanel();
        topButtonPanel.add(createRoomButton);
        topButtonPanel.add(backButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(topButtonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JPanel headerPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        headerPanel.add(new JLabel("방 이름"));
        headerPanel.add(new JLabel("인원"));
        headerPanel.add(new JLabel("상태"));
        headerPanel.add(new JLabel("입장"));

        roomContainer = new JPanel();
        roomContainer.setLayout(new BoxLayout(roomContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(roomContainer);

        JPanel listArea = new JPanel(new BorderLayout());
        listArea.add(headerPanel, BorderLayout.NORTH);
        listArea.add(scrollPane, BorderLayout.CENTER);

        add(listArea, BorderLayout.CENTER);
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void setCreateRoomAction(ActionListener listener) {
        createRoomButton.addActionListener(listener);
    }

    public void setRooms(List<RoomData> rooms, Consumer<RoomData> joinAction) {
        roomContainer.removeAll();

        for (RoomData room : rooms) {
            RoomRow row = new RoomRow(room);
            row.setJoinAction(joinAction);
            roomContainer.add(row);
        }

        roomContainer.revalidate();
        roomContainer.repaint();
    }
}
