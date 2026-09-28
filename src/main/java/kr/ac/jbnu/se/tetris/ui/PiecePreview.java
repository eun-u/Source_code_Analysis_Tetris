package kr.ac.jbnu.se.tetris.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.Piece;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** HOLD/NEXT 기본 모양 표시 및 배치 규칙·난수 생성 제외 */
public final class PiecePreview extends JPanel {
    private PieceType type = PieceType.EMPTY;
    public PiecePreview() { setPreferredSize(new Dimension(48, 60)); }
    public void setPiece(PieceType type) { this.type = type; repaint(); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (type == null || type == PieceType.EMPTY || type == PieceType.GARBAGE) return;
        Piece piece = new Piece(type);
        int minX = 10, maxX = -10, minY = 10, maxY = -10;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, piece.x(i)); maxX = Math.max(maxX, piece.x(i));
            minY = Math.min(minY, piece.y(i)); maxY = Math.max(maxY, piece.y(i));
        }
        int cell = Math.max(1, Math.min(14, Math.min(getWidth() / (maxX - minX + 1),
                getHeight() / (maxY - minY + 1))));
        int ox = (getWidth() - (maxX - minX + 1) * cell) / 2;
        int oy = (getHeight() - (maxY - minY + 1) * cell) / 2;
        for (int i = 0; i < 4; i++) {
            int x = ox + (piece.x(i) - minX) * cell;
            int y = oy + (piece.y(i) - minY) * cell;
            graphics.setColor(BoardView.colorFor(type));
            graphics.fillRect(x, y, cell - 1, cell - 1);
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRect(x, y, cell - 1, cell - 1);
        }
    }
}
