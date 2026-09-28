package kr.ac.jbnu.se.tetris.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.BoardState;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.Piece;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 불변 snapshot 렌더링 전용 뷰 및 충돌·고정·줄 제거 규칙 계산 제외 */
public final class BoardView extends JPanel {
    private static final Color[] COLORS = {
        Color.BLACK, new Color(204, 102, 102), new Color(102, 204, 102),
        new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204),
        new Color(102, 204, 204), new Color(218, 170, 0), new Color(120, 120, 125)
    };
    private GameState state;

    public BoardView() {
        setPreferredSize(new Dimension(320, 660));
        setBackground(new Color(28, 28, 32));
        setName("boardView");
    }
    public void setState(GameState state) { this.state = state; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (state == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            BoardState board = state.getBoard();
            int cell = Math.min(getWidth() / board.getWidth(), getHeight() / board.getHeight());
            if (cell < 1) return;
            int ox = (getWidth() - cell * board.getWidth()) / 2;
            int oy = (getHeight() - cell * board.getHeight()) / 2;
            for (int y = 0; y < board.getHeight(); y++) {
                for (int x = 0; x < board.getWidth(); x++) {
                    drawCell(g, board.getCell(x, y), ox + x * cell,
                            oy + (board.getHeight() - 1 - y) * cell, cell);
                }
            }
            Piece piece = state.getActivePiece();
            if (piece != null && piece.getType() != PieceType.EMPTY) {
                if (state.getGhostY() != state.getPieceY()) {
                    for (int i = 0; i < 4; i++) {
                        int x = state.getPieceX() + piece.x(i);
                        int y = state.getGhostY() - piece.y(i);
                        int sx = ox + x * cell, sy = oy + (board.getHeight() - 1 - y) * cell;
                        g.setColor(new Color(255, 255, 255, 45));
                        g.fillRect(sx + 1, sy + 1, cell - 2, cell - 2);
                        g.setColor(new Color(210, 210, 220, 130));
                        g.drawRect(sx + 1, sy + 1, cell - 3, cell - 3);
                    }
                }
                for (int i = 0; i < 4; i++) {
                    // 도메인의 아래쪽 y=0을 화면의 위쪽 y=0으로 변환
                    int x = state.getPieceX() + piece.x(i);
                    int y = state.getPieceY() - piece.y(i);
                    drawCell(g, piece.getType(), ox + x * cell,
                            oy + (board.getHeight() - 1 - y) * cell, cell);
                }
            }
            if (state.getStatus() == GameState.Status.PAUSED) {
                g.setColor(new Color(0, 0, 0, 175));
                g.fillRect(ox, oy, board.getWidth() * cell, board.getHeight() * cell);
                g.setColor(Color.WHITE);
                String text = "일시정지 · P로 계속";
                g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, getHeight() / 2);
            }
        } finally { g.dispose(); }
    }

    private static void drawCell(Graphics2D g, PieceType type, int x, int y, int size) {
        if (type == PieceType.EMPTY) {
            g.setColor(new Color(44, 44, 48));
            g.drawRect(x, y, size - 1, size - 1);
            return;
        }
        Color color = colorFor(type);
        g.setColor(color);
        g.fillRect(x + 1, y + 1, size - 2, size - 2);
        g.setColor(color.brighter());
        g.drawLine(x, y, x + size - 1, y);
        g.drawLine(x, y, x, y + size - 1);
        g.setColor(color.darker());
        g.drawLine(x + size - 1, y, x + size - 1, y + size - 1);
        g.drawLine(x, y + size - 1, x + size - 1, y + size - 1);
    }

    static Color colorFor(PieceType type) { return COLORS[type.ordinal()]; }
}
