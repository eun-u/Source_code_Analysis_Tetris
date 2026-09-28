package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;

/** 엔진 상태와 튜토리얼 안내를 표시하고 입력을 세션에 전달 */
public final class GamePanel extends JPanel implements Screen {
    private final BoardView board = new BoardView();
    private final JLabel lines = new JLabel("지운 줄: 0");
    private final JLabel status = new JLabel("준비");
    private final JLabel combo = new JLabel("Combo: 0");
    private final PieceQueuePanel queue = new PieceQueuePanel();
    private final JButton pause = new JButton("일시정지 (P)");
    private final Runnable enter;
    private final Runnable exit;
    private final JLabel instruction = new JLabel("튜토리얼", JLabel.CENTER);

    public GamePanel(Consumer<GameAction.Type> submit, Runnable togglePause,
                     Runnable home, Runnable enter, Runnable exit) {
        super(new BorderLayout(20, 12));
        this.enter = enter;
        this.exit = exit;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        instruction.setName("tutorialInstruction");
        add(instruction, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.add(lines);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(status);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(combo);
        sidebar.add(Box.createVerticalStrut(12));
        queue.setMaximumSize(new Dimension(215, 90));
        sidebar.add(queue);
        sidebar.add(Box.createVerticalStrut(24));
        pause.setName("pause");
        pause.addActionListener(event -> togglePause.run());
        JButton homeButton = new JButton("홈 (Esc)");
        homeButton.setName("home");
        homeButton.addActionListener(event -> home.run());
        // Space의 하드 드롭 전달을 위한 게임 중 버튼 키 포커스 제외
        pause.setFocusable(false);
        homeButton.setFocusable(false);
        sidebar.add(pause);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(homeButton);
        sidebar.add(Box.createVerticalStrut(32));
        sidebar.add(new JLabel("<html>← → 이동<br>↑ 왼쪽 회전<br>↓ 오른쪽 회전<br><br>D 한 칸 낙하<br>Space 즉시 낙하"
                + "<br>C HOLD (고정당 1회)<br><br>P 일시정지 / 계속<br>Esc 홈</html>"));
        add(sidebar, BorderLayout.EAST);
        setFocusable(true);
        GameKeyBindings.install(this, submit, togglePause, home);
    }

    public void setState(GameState state) {
        ScreenRouter.requireEdt();
        board.setState(state);
        lines.setText("지운 줄: " + state.getLinesCleared());
        combo.setText("Combo: " + Math.max(0, state.getCombo()));
        queue.setState(state);
        boolean paused = state.getStatus() == GameState.Status.PAUSED;
        pause.setText(paused ? "계속 (P)" : "일시정지 (P)");
        status.setText(paused ? "일시정지" : state.getStatus() == GameState.Status.GAME_OVER
                ? "게임 종료" : "플레이 중");
    }
    public void setInstruction(String text) { instruction.setText(text); }
    @Override public String getId() { return "game"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { enter.run(); requestFocusInWindow(); }
    @Override public void onExit() { exit.run(); }
}
