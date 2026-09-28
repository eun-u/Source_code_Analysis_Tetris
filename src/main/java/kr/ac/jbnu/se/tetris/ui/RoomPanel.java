package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;

/** 방 사본 표시와 준비 요청 전달용 기본 화면 */
public final class RoomPanel extends JPanel {
    private final JLabel title = new JLabel("방 연결 대기");
    private final JTextArea participants = new JTextArea();
    private final JButton ready = new JButton("준비");
    private final JButton leave = new JButton("나가기");
    private RoomState state;
    public RoomPanel(Consumer<RoomCommand> send) {
        super(new BorderLayout(8, 8));
        ready.setName("roomReady"); leave.setName("leaveRoom");
        participants.setEditable(false);
        add(title, BorderLayout.NORTH); add(participants, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout()); buttons.add(ready); buttons.add(leave);
        add(buttons, BorderLayout.SOUTH); ready.setEnabled(false); leave.setEnabled(false);
        ready.addActionListener(event -> {
            if (state != null) send.accept(RoomCommand.setReady(!state.getReadyByParticipantId().get(state.getLocalParticipantId())));
        });
        leave.addActionListener(event -> send.accept(RoomCommand.leaveRoom()));
    }
    public void setState(RoomState next) {
        ScreenRouter.requireEdt();
        state = next;
        if (next == null) {
            title.setText("방 연결 대기"); participants.setText(""); ready.setEnabled(false); leave.setEnabled(false); return;
        }
        title.setText("방 " + next.getRoomId() + " · " + next.getPhase());
        StringBuilder lines = new StringBuilder();
        for (Map.Entry<String, Boolean> participant : next.getReadyByParticipantId().entrySet()) {
            lines.append(participant.getKey()).append(participant.getKey().equals(next.getLocalParticipantId()) ? " (나)" : "")
                    .append(participant.getValue() ? " · 준비 완료" : " · 대기").append('\n');
        }
        participants.setText(lines.toString());
        ready.setEnabled(next.getPhase() == RoomState.Phase.WAITING);
        leave.setEnabled(next.getPhase() != RoomState.Phase.CLOSED);
        ready.setText(next.getReadyByParticipantId().get(next.getLocalParticipantId()) ? "준비 취소" : "준비");
    }
}
