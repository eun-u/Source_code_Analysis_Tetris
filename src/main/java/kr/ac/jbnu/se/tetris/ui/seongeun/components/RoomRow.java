package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

public class RoomRow extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient RoomData roomData;
    private final GameButton joinButton;

    public RoomRow (RoomData roomData) {
       this.roomData = roomData;

        setLayout(new GridLayout(1, 4, 10, 0)); // 1행 4열
        setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel roomNameLabel = new JLabel(roomData.getRoomName());  // 방이름
        JLabel playerCountLabel = new JLabel(roomData.getCurrentPlayers() + "/" + roomData.getMaxPlayers());  // 현재 참가 인원
        JLabel statusLabel = new JLabel(roomData.getStatus());   // Waiting, Playing 상태

        joinButton = new GameButton("입장"); // 입장 버튼
        joinButton.setEnabled(roomData.isJoinable());

        add(roomNameLabel);
        add(playerCountLabel);
        add(statusLabel);
        add(joinButton);
    }

    // 이 RoomData의 방을 눌렀다는 것을 전달
    public void setJoinAction(Consumer<RoomData> listener) {
        joinButton.addActionListener(e -> {
            listener.accept(roomData);
        });
    }

    // The join action captures the live room callback, so the row cannot be restored from a stream.
    private void writeObject(ObjectOutputStream output) throws IOException {
        throw new NotSerializableException(getClass().getName());
    }

    private void readObject(ObjectInputStream input) throws IOException {
        throw new NotSerializableException(getClass().getName());
    }
}
