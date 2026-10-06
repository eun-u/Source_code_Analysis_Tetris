package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.components.PlayerCard;

/** 성은 브랜치 WaitingRoomPanel의 회색 대기방·상대/나 카드 순서 */
public final class RoomPanel extends JPanel {
    private final JLabel title = new JLabel("Waiting Room", JLabel.CENTER);
    private final JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 40));
    private final JButton ready = new GameButton("READY");
    private final JButton leave = new GameButton("방 나가기");
    private final Consumer<RoomCommand> send;
    private RoomState state;
    private boolean matchFinished;

    public RoomPanel(Consumer<RoomCommand> send) {
        super(new BorderLayout());
        this.send = send;
        title.setName("roomTitle");
        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.CENTER);
        leave.setName("leaveRoom");
        leave.addActionListener(event -> send.accept(RoomCommand.leaveRoom()));
        top.add(leave, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        playerPanel.setBackground(Color.LIGHT_GRAY);
        add(playerPanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        ready.setName("roomReady");
        ready.addActionListener(event -> {
            if (state != null && canChangeReady()) {
                boolean current = Boolean.TRUE.equals(
                        state.getReadyByParticipantId().get(state.getLocalParticipantId()));
                send.accept(RoomCommand.setReady(!current));
            }
        });
        bottom.add(ready);
        add(bottom, BorderLayout.SOUTH);
        refreshActions();
    }

    public void setState(RoomState next) {
        ScreenRouter.requireEdt();
        state = next;
        if (next == null) matchFinished = false;
        title.setText(next == null ? "Waiting Room" : next.getRoomId());
        showPlayers();
        refreshActions();
    }

    /** 서버가 확정한 경기 종료 동안에만 다음 READY 요청 허용 */
    public void setMatchFinished(boolean finished) {
        ScreenRouter.requireEdt();
        matchFinished = finished;
        refreshActions();
    }

    private boolean canChangeReady() {
        return state != null && (state.getPhase() == RoomState.Phase.WAITING
                || state.getPhase() == RoomState.Phase.IN_MATCH && matchFinished);
    }

    private void refreshActions() {
        ready.setEnabled(canChangeReady());
        leave.setEnabled(state != null && state.getPhase() != RoomState.Phase.CLOSED);
        boolean localReady = state != null && Boolean.TRUE.equals(
                state.getReadyByParticipantId().get(state.getLocalParticipantId()));
        ready.setText(localReady ? "READY 취소" : "READY");
    }

    private void showPlayers() {
        playerPanel.removeAll();
        if (state != null) {
            int opponents = 0;
            for (Map.Entry<String, Boolean> entry : state.getReadyByParticipantId().entrySet()) {
                if (!entry.getKey().equals(state.getLocalParticipantId())) {
                    addCard(entry.getKey(), false, entry.getValue());
                    opponents++;
                }
            }
            if (opponents == 0) {
                PlayerCard waiting = new PlayerCard();
                waiting.setName("roomEmpty-opponent");
                waiting.setEmpty();
                playerPanel.add(waiting);
            }
            String local = state.getLocalParticipantId();
            addCard(local, true, state.getReadyByParticipantId().get(local));
        }
        playerPanel.revalidate();
        playerPanel.repaint();
    }

    private void addCard(String id, boolean local, boolean readyState) {
        PlayerCard card = new PlayerCard();
        card.setName("roomParticipant-" + id);
        card.setParticipant(id, local, readyState);
        playerPanel.add(card);
    }
}
