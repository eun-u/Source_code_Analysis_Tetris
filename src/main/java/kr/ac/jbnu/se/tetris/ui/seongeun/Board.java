package kr.ac.jbnu.se.tetris.ui.seongeun;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.BoardState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.Piece;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 성은 원본 Board의 색상과 좌표로 엔진 스냅샷만 표시하는 뷰. */
public class Board extends JPanel {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 22;
    private static final Color[] COLORS = {
        new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
        new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204),
        new Color(102, 204, 204), new Color(218, 170, 0), new Color(120, 120, 125)
    };

    private final JLabel statusbar;
    private GameState state;
    private Consumer<GameAction.Type> inputHandler;
    private Runnable pauseHandler;

    public Board(JLabel statusbar, boolean keyboardEnable) {
        this.statusbar = statusbar;
        setFocusable(true);
        if (keyboardEnable) addKeyListener(new TAdapter());
    }

    /** 원본 start 호출 흐름 유지. 게임 시작과 시간 흐름은 세션이 담당한다. */
    public void start() {
        requestFocusInWindow();
    }

    public void setState(GameState state) {
        this.state = state;
        if (statusbar != null && state != null) {
            if (state.getStatus() == GameState.Status.PAUSED) statusbar.setText("paused");
            else if (state.getStatus() == GameState.Status.GAME_OVER) statusbar.setText("game over");
            else statusbar.setText(String.valueOf(state.getLinesCleared()));
        }
        repaint();
    }

    public void setInputHandlers(Consumer<GameAction.Type> inputHandler, Runnable pauseHandler) {
        this.inputHandler = inputHandler;
        this.pauseHandler = pauseHandler;
    }

    private int squareWidth() { return getSize().width / BOARD_WIDTH; }
    private int squareHeight() { return getSize().height / BOARD_HEIGHT; }

    @Override public void paint(Graphics g) {
        super.paint(g);
        if (state == null || state.getBoard() == null) return;

        Dimension size = getSize();
        int boardTop = size.height - BOARD_HEIGHT * squareHeight();
        BoardState board = state.getBoard();
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                PieceType type = board.getCell(j, BOARD_HEIGHT - i - 1);
                if (type != PieceType.EMPTY)
                    drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), type);
            }
        }

        Piece piece = state.getActivePiece();
        if (piece != null && piece.getType() != PieceType.EMPTY) {
            for (int i = 0; i < 4; i++) {
                int x = state.getPieceX() + piece.x(i);
                int y = state.getPieceY() - piece.y(i);
                if (x >= 0 && x < BOARD_WIDTH && y >= 0 && y < BOARD_HEIGHT) {
                    drawSquare(g, x * squareWidth(), boardTop + (BOARD_HEIGHT - y - 1) * squareHeight(),
                            piece.getType());
                }
            }
        }
    }

    private void drawSquare(Graphics g, int x, int y, PieceType type) {
        Color color = COLORS[type.ordinal()];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + squareHeight() - 1, x, y);
        g.drawLine(x, y, x + squareWidth() - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + squareHeight() - 1, x + squareWidth() - 1, y + squareHeight() - 1);
        g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1, x + squareWidth() - 1, y + 1);
    }

    private final class TAdapter extends KeyAdapter {
        @Override public void keyPressed(KeyEvent event) {
            if (state == null) return;
            int key = event.getKeyCode();
            if (key == KeyEvent.VK_P) {
                if (pauseHandler != null && (state.getStatus() == GameState.Status.RUNNING
                        || state.getStatus() == GameState.Status.PAUSED)) pauseHandler.run();
                return;
            }
            if (state.getStatus() != GameState.Status.RUNNING || state.getActivePiece() == null
                    || inputHandler == null) return;

            GameAction.Type action;
            switch (key) {
                case KeyEvent.VK_LEFT: action = GameAction.Type.MOVE_LEFT; break;
                case KeyEvent.VK_RIGHT: action = GameAction.Type.MOVE_RIGHT; break;
                case KeyEvent.VK_DOWN: action = GameAction.Type.ROTATE_RIGHT; break;
                case KeyEvent.VK_UP: action = GameAction.Type.ROTATE_LEFT; break;
                case KeyEvent.VK_SPACE: action = GameAction.Type.HARD_DROP; break;
                case KeyEvent.VK_D: action = GameAction.Type.SOFT_DROP; break;
                case KeyEvent.VK_C: action = GameAction.Type.HOLD; break;
                default: return;
            }
            inputHandler.accept(action);
        }
    }
}
