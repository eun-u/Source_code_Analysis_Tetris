package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.resource.AssetManager;

/** 전투 snapshot 기반 HP·보드·대기열 표시 및 전투 규칙 계산 제외 */
public final class BattlePanel extends JPanel implements Screen {
    private final Map<String, ParticipantView> participantViewsById = new LinkedHashMap<String, ParticipantView>();
    private final JPanel participants = new JPanel(new GridLayout(1, 0, 20, 0));
    private String localParticipantId;
    private final JLabel status = new JLabel("몬스터 대전");
    private final JButton pause = new JButton("일시정지 (P)");
    private final JButton homeButton = new JButton("홈 (Esc)");
    private final JLabel controls = new JLabel("", JLabel.CENTER);
    private final Runnable enter;
    private final Runnable exit;
    private String encounter = "몬스터 대전";

    public BattlePanel(AssetManager assets, Consumer<GameAction.Type> submit, Runnable togglePause,
                       Runnable home, Runnable enter, Runnable exit) {
        super(new BorderLayout(12, 10));
        this.enter = enter; this.exit = exit;
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel(assets.getIcon("monster.normal", 36, 36)));
        toolbar.add(status);
        pause.setName("battlePause"); pause.setFocusable(false);
        pause.addActionListener(event -> togglePause.run());
        toolbar.add(pause);
        homeButton.setName("battleHome"); homeButton.setFocusable(false);
        homeButton.addActionListener(event -> home.run());
        toolbar.add(homeButton);
        add(toolbar, BorderLayout.NORTH);
        add(participants, BorderLayout.CENTER);
        controls.setText("← → 이동   ↑ ↓ 회전   D 한 칸 낙하   Space 즉시 낙하   C HOLD   P 일시정지");
        add(controls, BorderLayout.SOUTH);
        setFocusable(true);
        GameKeyBindings.install(this, submit, togglePause, home);
    }
    public void setState(BattleState state, boolean thinking) {
        String local = localParticipantId == null ? state.getParticipants().keySet().iterator().next() : localParticipantId;
        setState(state, local, true, thinking);
    }
    public void setState(BattleState state, String localId, boolean canPause, boolean thinking) {
        ScreenRouter.requireEdt();
        if (state == null || localId == null || state.getParticipant(localId) == null) {
            throw new IllegalArgumentException("Local participant must exist in displayed battle");
        }
        boolean changed = !participantViewsById.keySet().equals(state.getParticipants().keySet())
                || !localId.equals(localParticipantId);
        localParticipantId = localId;
        if (changed) {
            participantViewsById.clear(); participants.removeAll();
            for (String id : state.getParticipants().keySet()) {
                ParticipantView view = new ParticipantView(id.equals(localId) ? "playerBoard" : "board-" + id);
                participantViewsById.put(id, view); participants.add(view);
            }
            participants.revalidate();
        }
        for (ParticipantState participant : state.getParticipants().values()) {
            participantViewsById.get(participant.getId()).setState(participant,
                    participant.getId().equals(localId));
        }
        boolean paused = state.getStatus() == BattleState.Status.PAUSED;
        pause.setEnabled(canPause && state.getStatus() != BattleState.Status.FINISHED);
        homeButton.setText(canPause ? "홈 (Esc)" : "대전 나가기 (Esc)");
        controls.setText("← → 이동   ↑ ↓ 회전   D 한 칸 낙하   Space 즉시 낙하   C HOLD"
                + (canPause ? "   P 일시정지" : "   Esc 대전 나가기"));
        pause.setText(paused ? "계속 (P)" : "일시정지 (P)");
        status.setText(encounter + (paused ? " · 일시정지" : thinking ? " · 판단 중" : ""));
    }
    public int getParticipantViewCount() { return participantViewsById.size(); }
    public String getLocalParticipantId() { return localParticipantId; }
    public void setEncounter(String stage, String tier) {
        String label = "BOSS".equals(tier) ? "보스" : "ELITE".equals(tier) ? "엘리트" : "일반";
        encounter = stage + " · " + label;
    }
    public void setOnlineEncounter() { encounter = "Online PvP · 학생 대전"; }
    @Override public String getId() { return "battle"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { enter.run(); requestFocusInWindow(); }
    @Override public void onExit() { exit.run(); }

    private static final class ParticipantView extends JPanel {
        private final JLabel name = new JLabel("", JLabel.CENTER);
        private final JProgressBar hp = new JProgressBar();
        private final JLabel stats = new JLabel("", JLabel.CENTER);
        private final BoardView board = new BoardView();
        private final PieceQueuePanel queue = new PieceQueuePanel();
        private ParticipantView(String boardName) {
            super(new BorderLayout(4, 5));
            board.setName(boardName);
            JPanel header = new JPanel(new GridLayout(3, 1, 0, 4));
            header.add(name); header.add(hp); header.add(stats);
            hp.setStringPainted(true);
            add(header, BorderLayout.NORTH); add(board, BorderLayout.CENTER); add(queue, BorderLayout.SOUTH);
        }
        private void setState(ParticipantState participant, boolean local) {
            GameState state = participant.getGameState();
            name.setText(participant.getName() + (local ? " · 나" : ""));
            hp.setMaximum(participant.getMaxHp()); hp.setValue(participant.getHp());
            hp.setString("HP " + participant.getHp() + " / " + participant.getMaxHp());
            stats.setText("줄 " + state.getLinesCleared() + " · Combo " + Math.max(0, state.getCombo())
                    + " · 대기 Garbage " + state.getPendingGarbageLines());
            board.setState(state); queue.setState(state);
        }
    }
}
