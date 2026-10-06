package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.resource.AssetManager;
import kr.ac.jbnu.se.tetris.ui.components.CharacterView;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.components.HPBar;
import kr.ac.jbnu.se.tetris.ui.components.ItemSlot;

/** 성은 브랜치 BattlePanel의 보드·HP·캐릭터·슬롯 구성을 실제 전투 상태에 연결 */
public final class BattlePanel extends JPanel implements Screen {
    private final Map<String, ParticipantView> participantViewsById = new LinkedHashMap<String, ParticipantView>();
    private final JPanel participants = new JPanel(new GridLayout(1, 0, 12, 0));
    private final Icon participantIcon;
    private String localParticipantId;
    private final JLabel status = new JLabel("몬스터 대전");
    private final JButton pause = new GameButton("일시정지 (P)");
    private final JButton homeButton = new GameButton("홈 (Esc)");
    private final JLabel controls = new JLabel("", JLabel.CENTER);
    private final Runnable enter;
    private final Runnable exit;
    private String encounter = "몬스터 대전";

    public BattlePanel(AssetManager assets, Consumer<GameAction.Type> submit, Runnable togglePause,
                       Runnable home, Runnable enter, Runnable exit) {
        super(new BorderLayout(10, 8));
        this.enter = enter;
        this.exit = exit;
        this.participantIcon = assets.getIcon("character.default", 48, 48);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        status.setFont(status.getFont().deriveFont(18f));
        toolbar.add(status);
        pause.setName("battlePause");
        pause.setFocusable(false);
        pause.addActionListener(event -> togglePause.run());
        toolbar.add(pause);
        homeButton.setName("battleHome");
        homeButton.setFocusable(false);
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
            participantViewsById.clear();
            participants.removeAll();
            for (String id : state.getParticipants().keySet()) {
                ParticipantView view = new ParticipantView(id,
                        id.equals(localId) ? "playerBoard" : "board-" + id, participantIcon);
                participantViewsById.put(id, view);
                participants.add(view);
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
        homeButton.setPreferredSize(new Dimension(canPause ? 120 : 150, 35));
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
        private final HPBar hp = new HPBar();
        private final JLabel stats = new JLabel("", JLabel.CENTER);
        private final CharacterView character = new CharacterView();
        private final Icon portrait;
        private final BoardView board = new BoardView();
        private final PieceQueuePanel queue = new PieceQueuePanel();

        private ParticipantView(String id, String boardName, Icon portrait) {
            super(new BorderLayout(4, 5));
            this.portrait = portrait;
            setBorder(BorderFactory.createLineBorder(java.awt.Color.LIGHT_GRAY));
            hp.setName("participantHp-" + id);
            board.setName(boardName);
            JPanel header = new JPanel(new BorderLayout(4, 3));
            header.add(name, BorderLayout.NORTH);
            JPanel state = new JPanel(new GridLayout(2, 1, 0, 2));
            state.add(hp);
            state.add(stats);
            header.add(state, BorderLayout.CENTER);
            character.setCharacter(portrait, "참가자");
            header.add(character, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
            add(board, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            queue.setPreferredSize(new Dimension(0, 75));
            bottom.add(queue, BorderLayout.CENTER);
            JPanel itemArea = new JPanel(new BorderLayout(0, 2));
            itemArea.add(new JLabel("아이템 준비 중", JLabel.CENTER), BorderLayout.NORTH);
            JPanel slots = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            for (int slot = 0; slot < 3; slot++) slots.add(new ItemSlot());
            itemArea.add(slots, BorderLayout.CENTER);
            bottom.add(itemArea, BorderLayout.SOUTH);
            add(bottom, BorderLayout.SOUTH);
        }

        private void setState(ParticipantState participant, boolean local) {
            GameState state = participant.getGameState();
            name.setText(participant.getName() + (local ? " · 나" : ""));
            character.setCharacter(portrait, local ? "나" : "상대");
            hp.setHP(participant.getHp(), participant.getMaxHp());
            stats.setText("<html><center>줄 " + state.getLinesCleared()
                    + " · Combo " + Math.max(0, state.getCombo())
                    + "<br>대기 Garbage " + state.getPendingGarbageLines() + "</center></html>");
            board.setState(state);
            queue.setState(state);
        }
    }
}
