package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 낡은 한국 대학 강의동의 무채색 콘크리트에 미노 색을 얹은 공통 블록 스킨. */
public final class ConcreteBlockSkin {
    private static final BufferedImage TILE = load("concrete-tile.png");
    private static final BufferedImage ORE = load("ore-tile.png");

    private ConcreteBlockSkin() { }

    public static Color color(PieceType type) {
        switch (type) {
            case I: return new Color(91, 193, 202);
            case O: return new Color(222, 183, 90);
            case T: return new Color(170, 139, 196);
            case S: return new Color(105, 183, 145);
            case Z: return new Color(204, 124, 111);
            case J: return new Color(111, 145, 196);
            case L: return new Color(211, 154, 99);
            case GARBAGE: return new Color(93, 100, 105);
            default: return new Color(93, 100, 105);
        }
    }

    public static void paint(Graphics2D graphics, int x, int y, int size,
                             PieceType type, boolean ghost, boolean ore) {
        if (size < 2 || type == PieceType.EMPTY) return;
        Color accent = color(type);
        if (ghost) {
            graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 145));
            graphics.drawRect(x + 2, y + 2, Math.max(1, size - 5), Math.max(1, size - 5));
            if (ore) paintOreMark(graphics, x, y, size, true);
            return;
        }
        graphics.setColor(new Color(22, 29, 34));
        graphics.fillRect(x, y, size, size);
        int inset = size >= 8 ? 1 : 0;
        int inner = size - inset * 2;
        BufferedImage image = ore ? ORE : TILE;
        if (image != null && size >= 15) {
            Object previous = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(image, x + inset, y + inset, inner, inner, null);
            if (previous != null) graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);
        } else {
            graphics.setColor(ore ? new Color(105, 102, 89) : new Color(132, 137, 138));
            graphics.fillRect(x + inset, y + inset, inner, inner);
            graphics.setColor(new Color(205, 207, 200, 70));
            graphics.drawLine(x + inset + 1, y + inset + 1, x + size - inset - 2, y + inset + 1);
        }
        graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), ore ? 25 : 68));
        graphics.fillRect(x + inset, y + inset, inner, inner);
        graphics.setColor(ore ? new Color(255, 222, 116) : accent);
        graphics.drawLine(x + 1, y + 1, x + size - 2, y + 1);
        graphics.drawLine(x + 1, y + 1, x + 1, y + size - 2);
        if (ore) paintOreMark(graphics, x, y, size, false);
        graphics.setColor(new Color(18, 25, 32, 185));
        graphics.drawLine(x + 1, y + size - 2, x + size - 2, y + size - 2);
        graphics.drawLine(x + size - 2, y + 1, x + size - 2, y + size - 2);
    }

    private static void paintOreMark(Graphics2D graphics, int x, int y, int size, boolean ghost) {
        if (size < 8) return;
        int cx = x + size / 2, cy = y + size / 2;
        int radius = Math.max(2, size / 5);
        graphics.setColor(ghost ? new Color(255, 209, 102, 170) : new Color(62, 39, 19));
        graphics.fillPolygon(new int[] { cx, cx + radius + 1, cx, cx - radius - 1 },
                new int[] { cy - radius - 1, cy, cy + radius + 1, cy }, 4);
        graphics.setColor(ghost ? new Color(255, 246, 189, 220) : new Color(255, 225, 103));
        graphics.fillPolygon(new int[] { cx, cx + radius, cx, cx - radius },
                new int[] { cy - radius, cy, cy + radius, cy }, 4);
        if (ghost) return;
        graphics.setColor(new Color(255, 253, 221));
        graphics.fillRect(cx - 1, cy - 1, Math.max(2, size / 8), Math.max(2, size / 8));
        graphics.setColor(new Color(255, 228, 133));
        graphics.drawRect(x + 2, y + 2, size - 5, size - 5);
        if (size >= 16) {
            graphics.setColor(new Color(255, 249, 199));
            graphics.fillRect(x + 1, y + 1, 3, 3);
            graphics.fillRect(x + size - 4, y + size - 4, 2, 2);
        }
    }

    private static BufferedImage load(String name) {
        try {
            java.net.URL resource = ConcreteBlockSkin.class.getResource("/ui/campus-rpg/" + name);
            return resource == null ? null : ImageIO.read(resource);
        } catch (IOException error) { return null; }
    }
}
