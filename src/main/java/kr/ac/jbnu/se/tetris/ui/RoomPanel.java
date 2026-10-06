package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.components.PlayerCard;

/** 성은 브랜치 WaitingRoomPanel의 참가자 카드를 서버 RoomState에 연결 */
public final class RoomPanel extends JPanel {
    private final JLabel title = new JLabel("방 연결 대기", JLabel.CENTER);
    private final JPanel cards = new JPanel(new GridLayout(1, 2, 12, 12));
    private final JButton ready = new GameButton("준비");
    private final JButton leave = new GameButton("나가기");
    private RoomState state;

    public RoomPanel(Consumer<RoomCommand> send) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        title.setName("roomTitle");
        title.setFont(title.getFont().deriveFont(18f));
        add(title, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout());
        ready.setName("roomReady");
        leave.setName("leaveRoom");
        buttons.add(ready);
        buttons.add(leave);
        add(buttons, BorderLayout.SOUTH);
        ready.setEnabled(false);
        leave.setEnabled(false);
        ready.addActionListener(event -> {
            if (state != null && state.getPhase() == RoomState.Phase.WAITING) {
                boolean current = Boolean.TRUE.equals(state.getReadyByParticipantId().get(state.getLocalParticipantId()));
                send.accept(RoomCommand.setReady(!current));
            }
        });
        leave.addActionListener(event -> send.accept(RoomCommand.leaveRoom()));
        showCards(null);
    }

    public void setState(RoomState next) {
        ScreenRouter.requireEdt();
        state = next;
        showCards(next);
        if (next == null) {
            title.setText("방 연결 대기");
            ready.setEnabled(false);
            leave.setEnabled(false);
            ready.setText("준비");
            return;
        }
        String phase = next.getPhase() == RoomState.Phase.WAITING ? "대기 중"
                : next.getPhase() == RoomState.Phase.IN_MATCH ? "대전 중" : "종료";
        title.setText("방 " + next.getRoomId() + " · " + phase);
        ready.setEnabled(next.getPhase() == RoomState.Phase.WAITING);
        leave.setEnabled(next.getPhase() != RoomState.Phase.CLOSED);
        boolean current = Boolean.TRUE.equals(next.getReadyByParticipantId().get(next.getLocalParticipantId()));
        ready.setText(current ? "준비 취소" : "준비");
    }

    private void showCards(RoomState room) {
        cards.removeAll();
        int count = 0;
        int visibleSlots = room == null || room.getReadyByParticipantId().size() <= 2 ? 2 : 4;
        cards.setLayout(new GridLayout(visibleSlots == 2 ? 1 : 2, 2, 12, 12));
        if (room != null) {
            for (Map.Entry<String, Boolean> participant : room.getReadyByParticipantId().entrySet()) {
                PlayerCard card = new PlayerCard();
                card.setName("roomParticipant-" + participant.getKey());
                card.setParticipant(participant.getKey(),
                        participant.getKey().equals(room.getLocalParticipantId()), participant.getValue());
                cards.add(card);
                count++;
            }
        }
        for (int slot = count; slot < visibleSlots; slot++) {
            PlayerCard empty = new PlayerCard();
            empty.setName("roomEmpty-" + slot);
            empty.setEmpty();
            cards.add(empty);
        }
        cards.revalidate();
        cards.repaint();
    }
}
