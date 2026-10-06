package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 LocalGamePanel의 중앙 240×440 보드·상단 Back·하단 상태 */
public final class GamePanel extends JPanel implements Screen {
    private final BoardView board = new BoardView();
    private final JLabel title = new JLabel("Local Game", JLabel.CENTER);
    private final JLabel status = new JLabel("0", JLabel.CENTER);
    private final JLabel instruction = new JLabel("", JLabel.CENTER);
    private final JButton pause = new GameButton("일시정지");
    private final Runnable enter;
    private final Runnable exit;

    public GamePanel(Consumer<GameAction.Type> submit, Runnable togglePause,
                     Runnable home, Runnable enter, Runnable exit) {
        super(new BorderLayout());
        this.enter = enter;
        this.exit = exit;
        JPanel top = new JPanel(new BorderLayout());
        JButton back = new GameButton("Back");
        back.setName("home");
        back.addActionListener(event -> home.run());
        back.setFocusable(false);
        pause.setName("pause");
        pause.addActionListener(event -> togglePause.run());
        pause.setFocusable(false);
        top.add(pause, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        top.add(back, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        Dimension boardSize = new Dimension(240, 440);
        board.setPreferredSize(boardSize);
        board.setMinimumSize(boardSize);
        board.setMaximumSize(boardSize);
        board.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JPanel boardArea = new JPanel(new GridBagLayout());
        boardArea.add(board);
        add(boardArea, BorderLayout.CENTER);

        instruction.setName("tutorialInstruction");
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(instruction, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        setFocusable(true);
        GameKeyBindings.install(this, submit, togglePause, home);
    }

    public void setMode(String name) { title.setText("Local Mode - " + name); }
    public void setState(GameState state) {
        ScreenRouter.requireEdt();
        board.setState(state);
        boolean paused = state.getStatus() == GameState.Status.PAUSED;
        status.setText("Line : " + state.getLinesCleared()
                + (paused ? " · 일시정지" : state.getStatus() == GameState.Status.GAME_OVER ? " · Game Over" : ""));
        pause.setText(paused ? "계속" : "일시정지");
    }
    public void setInstruction(String text) { instruction.setText(text); }
    @Override public String getId() { return "game"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { enter.run(); requestFocusInWindow(); }
    @Override public void onExit() { exit.run(); }
}
