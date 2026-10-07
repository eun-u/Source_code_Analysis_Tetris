package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.core.Piece;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** HOLD와 NEXT를 실제 미노 모양으로 보여 주는 작은 픽셀 창. */
public final class MiniPiecePreview extends JComponent {
    private final String title;
    private PieceType type;
    private boolean item;
    public MiniPiecePreview(String title) {
        this.title = title;
        setPreferredSize(new Dimension(68, 70));
        setMinimumSize(new Dimension(66, 68));
        setMaximumSize(new Dimension(68, 70));
        setToolTipText(title);
    }
    public void setPiece(PieceType type, boolean item) {
        this.type = type == PieceType.EMPTY ? null : type;
        this.item = item;
        repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(UniversityPixelTheme.BLACK); g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(UniversityPixelTheme.PANEL); g.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
            g.setColor(UniversityPixelTheme.LINE); g.drawRect(5, 5, getWidth() - 11, getHeight() - 11);
            g.setFont(UniversityPixelTheme.font(12, Font.BOLD));
            g.setColor(UniversityPixelTheme.GOLD); g.drawString(title, 7, 15);
            if (type == null) {
                g.setColor(UniversityPixelTheme.TEXT_SUB);
                g.drawString("없음", 19, 46);
                return;
            }
            Piece piece = new Piece(type);
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (int index = 0; index < 4; index++) {
                minX = Math.min(minX, piece.x(index)); maxX = Math.max(maxX, piece.x(index));
                minY = Math.min(minY, piece.y(index)); maxY = Math.max(maxY, piece.y(index));
            }
            int cell = 11, width = (maxX - minX + 1) * cell, height = (maxY - minY + 1) * cell;
            int left = (getWidth() - width) / 2, top = 22 + (getHeight() - 22 - height) / 2;
            for (int index = 0; index < 4; index++) {
                int x = left + (piece.x(index) - minX) * cell;
                int y = top + (maxY - piece.y(index)) * cell;
                ConcreteBlockSkin.paint(g, x, y, cell, type, false, false);
            }
            if (item) {
                g.setColor(UniversityPixelTheme.GOLD);
                g.fillRect(getWidth() - 12, 3, 6, 6);
            }
        } finally { g.dispose(); }
    }
}
