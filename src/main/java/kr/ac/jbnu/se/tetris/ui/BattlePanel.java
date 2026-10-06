package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.resource.AssetManager;
import kr.ac.jbnu.se.tetris.ui.components.CharacterView;
import kr.ac.jbnu.se.tetris.ui.components.FeverBar;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.components.HPBar;
import kr.ac.jbnu.se.tetris.ui.components.ItemSlot;

/** 성은 브랜치 BattlePanel의 상대 왼쪽·사용자 오른쪽 세로 전투 구성 */
public final class BattlePanel extends JPanel implements Screen {
    private final Map<String, ParticipantView> participantViewsById = new LinkedHashMap<String, ParticipantView>();
    private final Map<String, ScaledParticipant> scaledViewsById = new LinkedHashMap<String, ScaledParticipant>();
    private final JPanel battleArea = new JPanel(new GridBagLayout());
    private final JLabel title = new JLabel("BATTLE", JLabel.CENTER);
    private final JPanel bottom = new JPanel();
    private final JScrollPane scroll;
    private final JButton pause = new GameButton("일시정지");
    private final JButton homeButton = new GameButton("홈");
    private final Runnable enter;
    private final Runnable exit;
    private String localParticipantId;
    private String encounter = "몬스터 대전";

    public BattlePanel(AssetManager assets, Consumer<GameAction.Type> submit, Runnable togglePause,
                       Runnable home, Runnable enter, Runnable exit) {
        super(new BorderLayout());
        this.enter = enter;
        this.exit = exit;
        add(title, BorderLayout.NORTH);
        scroll = new JScrollPane(battleArea,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        pause.setName("battlePause");
        pause.setFocusable(false);
        pause.addActionListener(event -> togglePause.run());
        bottom.add(pause);
        homeButton.setName("battleHome");
        homeButton.setFocusable(false);
        homeButton.addActionListener(event -> home.run());
        bottom.add(homeButton);
        add(bottom, BorderLayout.SOUTH);
        setFocusable(true);
        GameKeyBindings.install(this, submit, togglePause, home);
    }

    @Override public void doLayout() {
        if (!scaledViewsById.isEmpty() && getHeight() > 0) {
            int logicalHeight = 0;
            for (ScaledParticipant view : scaledViewsById.values()) {
                logicalHeight = Math.max(logicalHeight, view.getLogicalHeight());
            }
            // 3~4인 가로 스크롤바가 생기는 경우에도 마지막 보드 행을 항상 확보
            int available = Math.max(1, getHeight() - title.getPreferredSize().height
                    - bottom.getPreferredSize().height - 22);
            double scale = Math.min(1.0, (double) available / (logicalHeight + 40));
            GridBagLayout layout = (GridBagLayout) battleArea.getLayout();
            for (ScaledParticipant view : scaledViewsById.values()) {
                view.setScale(scale);
                GridBagConstraints constraints = layout.getConstraints(view);
                constraints.insets = view.scaledInsets(scale);
                layout.setConstraints(view, constraints);
            }
        }
        super.doLayout();
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
            scaledViewsById.clear();
            battleArea.removeAll();
            int column = 0;
            for (ParticipantState participant : state.getParticipants().values()) {
                if (!participant.getId().equals(localId)) {
                    addParticipant(participant.getId(), false, column++);
                }
            }
            addParticipant(localId, true, column);
            battleArea.revalidate();
        }
        for (ParticipantState participant : state.getParticipants().values()) {
            participantViewsById.get(participant.getId()).setState(participant);
            scaledViewsById.get(participant.getId()).refreshLogicalSize();
        }
        battleArea.revalidate();
        boolean paused = state.getStatus() == BattleState.Status.PAUSED;
        pause.setEnabled(canPause && state.getStatus() != BattleState.Status.FINISHED);
        pause.setText(paused ? "계속" : "일시정지");
        homeButton.setText(canPause ? "홈" : "대전 나가기");
        title.setToolTipText(encounter + (paused ? " · 일시정지" : thinking ? " · 판단 중" : ""));
    }

    private void addParticipant(String id, boolean local, int column) {
        ParticipantView view = new ParticipantView(id, local);
        Insets originalInsets = local ? new Insets(20, 40, 20, 20)
                : new Insets(20, 20, 20, column == 0 ? 40 : 20);
        ScaledParticipant scaled = new ScaledParticipant(view, originalInsets);
        participantViewsById.put(id, view);
        scaledViewsById.put(id, scaled);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.insets = originalInsets;
        battleArea.add(scaled, constraints);
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

    /** 원본 열의 논리 좌표를 유지한 채 화면 높이에 맞게 일괄 축소 */
    private static final class ScaledParticipant extends JPanel {
        private final ParticipantView view;
        private final Insets originalInsets;
        private Dimension logicalSize;
        private double scale = 1.0;

        private ScaledParticipant(ParticipantView view, Insets originalInsets) {
            this.view = view;
            this.originalInsets = originalInsets;
            this.logicalSize = view.getPreferredSize();
            setLayout(null);
            add(view);
        }

        private int getLogicalHeight() { return logicalSize.height; }
        private void refreshLogicalSize() {
            // setText 직후 EDT의 지연 revalidate 전에 BoxLayout의 빈 이름 캐시 제거
            ((BoxLayout) view.getLayout()).invalidateLayout(view);
            view.invalidate();
            Dimension next = view.getPreferredSize();
            if (!logicalSize.equals(next)) {
                logicalSize = next;
                revalidate();
            }
        }
        private void setScale(double next) {
            if (Math.abs(scale - next) > 0.0001) {
                scale = next;
                revalidate();
                repaint();
            }
        }
        private Insets scaledInsets(double factor) {
            return new Insets(round(originalInsets.top * factor), round(originalInsets.left * factor),
                    round(originalInsets.bottom * factor), round(originalInsets.right * factor));
        }
        private static int round(double value) { return (int) Math.round(value); }
        @Override public Dimension getPreferredSize() {
            return new Dimension(Math.max(1, round(logicalSize.width * scale)),
                    Math.max(1, round(logicalSize.height * scale)));
        }
        @Override public void doLayout() {
            view.setBounds(0, 0, logicalSize.width, logicalSize.height);
            view.doLayout();
        }
        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D scaled = (Graphics2D) graphics.create();
            try {
                scaled.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                scaled.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                scaled.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                scaled.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);
                scaled.scale(scale, scale);
                super.paintChildren(scaled);
            } finally { scaled.dispose(); }
        }
    }

    private static final class ParticipantView extends JPanel {
        private final JLabel name = new JLabel("", JLabel.CENTER);
        private final HPBar hp = new HPBar(100, 100);
        private final FeverBar fever = new FeverBar();
        private final CharacterView character = new CharacterView();
        private final JPanel characterArea = new JPanel(new BorderLayout());
        private final JPanel items = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        private final BoardView board = new BoardView();
        private final JLabel status = new JLabel("0", JLabel.CENTER);

        private ParticipantView(String id, boolean local) {
            int width = local ? 240 : 180;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            name.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(name);
            add(Box.createVerticalStrut(5));
            fixed(hp, width, 25);
            hp.setName("participantHp-" + id);
            add(hp);
            add(Box.createVerticalStrut(10));
            fixed(fever, width, 25);
            add(fever);
            add(Box.createVerticalStrut(10));
            character.setCharacter(null, "—");
            characterArea.setAlignmentX(Component.CENTER_ALIGNMENT);
            characterArea.add(character, BorderLayout.CENTER);
            add(characterArea);
            add(Box.createVerticalStrut(10));
            items.setAlignmentX(Component.CENTER_ALIGNMENT);
            for (String key : new String[] {"Q", "W", "E"}) items.add(new ItemSlot(key));
            add(items);
            add(Box.createVerticalStrut(10));
            Dimension boardSize = new Dimension(width, local ? 440 : 360);
            board.setName(local ? "playerBoard" : "board-" + id);
            board.setPreferredSize(boardSize);
            board.setMinimumSize(boardSize);
            board.setMaximumSize(boardSize);
            board.setBorder(BorderFactory.createLineBorder(local ? Color.BLACK : Color.GRAY));
            board.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(board);
            add(Box.createVerticalStrut(5));
            status.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(status);
        }

        private static void fixed(JPanel panel, int width, int height) {
            Dimension size = new Dimension(width, height);
            panel.setPreferredSize(size);
            panel.setMaximumSize(size);
            panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        private void setState(ParticipantState participant) {
            name.setText(participant.getName());
            hp.setHP(participant.getHp(), participant.getMaxHp());
            board.setState(participant.getGameState());
            status.setText(String.valueOf(participant.getGameState().getLinesCleared()));
        }
    }
}
