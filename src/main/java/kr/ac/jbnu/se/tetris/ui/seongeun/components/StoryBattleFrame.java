package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/** Purely decorative campus-RPG frames. The game boards and controls stay live components. */
public final class StoryBattleFrame {
    private static final Color EDGE = new Color(0x422B22);
    private static final Color WOOD = new Color(0xD7A975);
    private static final Color LIGHT_WOOD = new Color(0xF6D7A5);
    private static final Color INNER = new Color(0x17222D);
    private static final Color LEAF = new Color(0x78C654);
    private static final Color LEAF_DARK = new Color(0x366E37);

    private StoryBattleFrame() { }

    /** A labeled wooden frame surrounding (and never painting over) an active board. */
    public static void drawBoard(Graphics2D graphics, Rectangle board, String caption) {
        if (board.width <= 0 || board.height <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = board.x - 11, y = board.y - 11;
            int w = board.width + 22, h = board.height + 22;
            g.setColor(new Color(0, 0, 0, 67));
            g.fillRoundRect(x + 3, y + 8, w, h, 23, 23);
            g.setColor(EDGE);
            g.fillRoundRect(x, y, w, h, 24, 24);
            g.setColor(WOOD);
            g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 19, 19);
            g.setColor(LIGHT_WOOD);
            g.drawRoundRect(x + 6, y + 5, w - 13, h - 12, 14, 14);
            g.setColor(INNER);
            g.fillRoundRect(x + 9, y + 9, w - 18, h - 18, 9, 9);

            int labelWidth = Math.max(98, Math.min(160, board.width - 10));
            int labelHeight = 37, labelX = board.x + (board.width - labelWidth) / 2;
            int labelY = y - 26;
            g.setColor(new Color(0, 0, 0, 53));
            g.fillRoundRect(labelX + 2, labelY + 4, labelWidth, labelHeight, 19, 19);
            g.setColor(EDGE);
            g.fillRoundRect(labelX, labelY, labelWidth, labelHeight, 20, 20);
            g.setColor(LIGHT_WOOD);
            g.fillRoundRect(labelX + 3, labelY + 3, labelWidth - 6, labelHeight - 7, 16, 16);
            g.setColor(new Color(0xFFF2D7));
            g.drawRoundRect(labelX + 5, labelY + 5, labelWidth - 11, labelHeight - 11, 13, 13);
            g.setColor(EDGE);
            g.setFont(UniversityPixelTheme.font(16, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(caption, labelX + (labelWidth - fm.stringWidth(caption)) / 2,
                    labelY + (labelHeight - fm.getHeight()) / 2 + fm.getAscent() - 2);
            drawSprig(g, labelX - 6, labelY + 13, -1);
            drawSprig(g, labelX + labelWidth + 6, labelY + 13, 1);
        } finally { g.dispose(); }
    }

    /** Compact wooden HOLD/NEXT shell with a darker inner surface. */
    public static void drawPreview(Graphics2D graphics, int width, int height) {
        if (width < 20 || height < 20) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(EDGE);
            g.fillRoundRect(0, 0, width, height, 24, 24);
            g.setColor(WOOD);
            g.fillRoundRect(3, 3, width - 6, height - 6, 19, 19);
            g.setColor(LIGHT_WOOD);
            g.drawRoundRect(5, 5, width - 11, height - 11, 16, 16);
            g.setColor(new Color(0x23382F));
            g.fillRoundRect(8, 27, width - 16, Math.max(0, height - 35), 11, 11);
            g.setColor(EDGE);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(8, 27, width - 17, Math.max(0, height - 36), 10, 10);
            drawSprig(g, width - 11, 8, 1);
        } finally { g.dispose(); }
    }

    /** Wooden FEVER frame; the bar and percentage are painted by their live components. */
    public static void drawFever(Graphics2D graphics, int width, int height) {
        if (width < 20 || height < 20) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(EDGE);
            g.fillRoundRect(0, 0, width, height, 24, 24);
            g.setColor(WOOD);
            g.fillRoundRect(3, 3, width - 6, height - 6, 20, 20);
            g.setColor(new Color(0x293E34));
            g.fillRoundRect(7, 7, width - 14, height - 14, 17, 17);
            g.setColor(new Color(0xADCEAB));
            g.drawRoundRect(9, 9, width - 19, height - 19, 14, 14);
            // Star accent is deliberately a vector: no external PNG dependency.
            int cx = 26, cy = Math.max(24, Math.min(48, (height - 64) / 2 + 17));
            int outer = 12, inner = 6;
            int[] xs = new int[10], ys = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + Math.PI * i / 5;
                int radius = (i % 2 == 0) ? outer : inner;
                xs[i] = cx + (int) Math.round(Math.cos(angle) * radius);
                ys[i] = cy + (int) Math.round(Math.sin(angle) * radius);
            }
            g.setColor(EDGE);
            g.fillPolygon(xs, ys, 10);
            g.setColor(new Color(0xFFE269));
            g.fillPolygon(xs, ys, 10);
            drawSprig(g, 8, height - 7, -1);
            drawSprig(g, width - 7, height - 8, 1);
        } finally { g.dispose(); }
    }

    private static void drawSprig(Graphics2D g, int x, int y, int direction) {
        AffineTransform transform = g.getTransform();
        g.translate(x, y);
        g.scale(direction, 1);
        g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(LEAF_DARK);
        g.drawLine(-13, 7, 9, -3);
        g.setColor(LEAF);
        g.fillOval(-12, -13, 16, 12);
        g.fillOval(0, -11, 15, 11);
        g.setColor(LEAF_DARK);
        g.drawOval(-12, -13, 16, 12);
        g.drawOval(0, -11, 15, 11);
        g.setTransform(transform);
    }
}
