package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** 전투 화면을 강의동 외벽처럼 감싸는 배경, 기둥, 보드 테두리를 그린다. */
final class BattleFacade {
    private static final Color BODY = new Color(0xB9B7AB);
    static final Color EDGE = new Color(0x888D87);
    private static final Color LIGHT = new Color(0xE5E2D7);
    private static final BufferedImage UNIVERSITY_BATTLE_BG = readBackdrop(
            "/ui/campus-rpg/university-battle-bg.png");
    private static final BufferedImage UNIVERSITY_BG = readBackdrop("/ui/campus-rpg/university-bg.png");
    private static final BufferedImage GRADUATION_BG = readBackdrop("/ui/campus-rpg/graduation-bg.png");
    private static final BufferedImage EMPLOYMENT_BG = readBackdrop("/ui/campus-rpg/employment-bg.png");

    private BattleFacade() { }

    /** 시즌 배경이 없으면 장(chapter)에 맞는 기본 배경을 고른다. */
    static BufferedImage pveBackdrop(BufferedImage skinBackdrop, String chapter) {
        return skinBackdrop != null ? skinBackdrop
                : "graduation".equals(chapter) ? GRADUATION_BG
                : "employment".equals(chapter) ? EMPLOYMENT_BG
                : onlineBackdrop();
    }

    static BufferedImage onlineBackdrop() {
        return UNIVERSITY_BATTLE_BG != null ? UNIVERSITY_BATTLE_BG : UNIVERSITY_BG;
    }

    static void paintCommonShell(Graphics2D g, int width, int height, int margin, int side, int top, int bottom,
                                 BufferedImage backdrop) {
        if (backdrop != null) {
            paintBackdropCover(g, backdrop, width, height);
            // 프레임 바깥 풍경은 남기되 화면의 주인공인 보드와 전투원보다 조용하게 둔다.
            g.setColor(new Color(231, 225, 213, 66));
            g.fillRect(0, 0, width, height);
        }
        int shellW = Math.max(0, width - margin * 2);
        int shellH = Math.max(0, height - margin * 2);
        if (shellW < 40 || shellH < 40) return;
        int bodyBottom = height - bottom;
        // 외벽, 상인방, 하단 콘크리트 턱은 세 칸을 하나의 건물처럼 연결한다.
        g.setColor(EDGE);
        g.fillRect(margin, margin, shellW, shellH);
        g.setColor(BODY);
        g.fillRect(margin + 2, margin + 2, shellW - 4, shellH - 4);
        g.setColor(LIGHT);
        g.fillRect(margin + 3, margin + 3, shellW - 6, top - margin - 7);
        g.setColor(UniversityPixelTheme.PANEL_LIGHT);
        g.fillRect(side, top, Math.max(0, width - side * 2),
                Math.max(0, bodyBottom - top));
        g.setColor(LIGHT);
        g.fillRect(margin + 3, bodyBottom, shellW - 6, Math.max(0, height - margin - bodyBottom - 3));
        paintConcreteTexture(g, margin + 3, margin + 3, shellW - 6, top - margin - 6);
        paintConcreteTexture(g, margin + 3, bodyBottom, shellW - 6,
                Math.max(0, height - margin - bodyBottom - 3));
        g.setColor(EDGE);
        g.drawLine(margin + 2, top - 3, margin + shellW - 3, top - 3);
        g.drawLine(margin + 2, bodyBottom, margin + shellW - 3, bodyBottom);
        g.setColor(new Color(0xF6F2E8));
        g.drawLine(margin + 3, margin + 3, margin + shellW - 4, margin + 3);
    }

    static void paintPillar(Graphics2D g, int x, int width, int top, int bottom) {
        g.setColor(EDGE);
        g.fillRect(x, top, width, bottom - top);
        g.setColor(BODY);
        g.fillRect(x + 2, top, Math.max(0, width - 4), bottom - top);
        g.setColor(LIGHT);
        g.drawLine(x + 2, top + 2, x + 2, bottom - 3);
        g.setColor(new Color(0xA1A49D));
        g.drawLine(x + width - 3, top + 2, x + width - 3, bottom - 3);
    }

    private static void paintConcreteTexture(Graphics2D g, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        g.setColor(new Color(77, 79, 72, 20));
        for (int row = 5; row < height - 3; row += 11)
            for (int col = 7 + ((row * 7) % 13); col < width - 3; col += 19)
                g.fillRect(x + col, y + row, 2, 1);
        g.setColor(new Color(255, 255, 255, 52));
        for (int col = 47; col < width - 10; col += 113)
            g.drawLine(x + col, y + 2, x + col, y + Math.min(height - 3, 7));
    }

    static void paintBackdropCover(Graphics2D g, BufferedImage image, int width, int height) {
        if (width <= 0 || height <= 0) return;
        double cropScale = Math.min((double) image.getWidth() / width,
                (double) image.getHeight() / height);
        int sourceW = Math.max(1, (int) Math.round(width * cropScale));
        int sourceH = Math.max(1, (int) Math.round(height * cropScale));
        int sourceX = (image.getWidth() - sourceW) / 2;
        int sourceY = (image.getHeight() - sourceH) / 2;
        g.drawImage(image, 0, 0, width, height,
                sourceX, sourceY, sourceX + sourceW, sourceY + sourceH, null);
    }

    private static BufferedImage readBackdrop(String resource) {
        try (InputStream stream = BattleFacade.class.getResourceAsStream(resource)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException exception) { return null; }
    }

    /** 칸은 그대로 두고 바깥 여백 5px만 강의동 외벽처럼 감싼다. */
    static void paintBoardFacade(Graphics2D g, Rectangle board) {
        int x = board.x - 5, y = board.y - 7;
        int width = board.width + 10, height = board.height + 14;
        g.setColor(BODY);
        g.fillRect(x, y, width, height);
        g.setColor(EDGE);
        g.drawRect(x, y, width - 1, height - 1);
        g.setColor(LIGHT);
        g.drawLine(x + 1, y + 1, x + width - 2, y + 1);
        g.drawLine(x + 1, y + height - 3, x + width - 2, y + height - 3);
        // 윗쪽 콘크리트 줄눈만 남겨 화면 전체에 창문 무늬가 반복되지 않게 한다.
        g.setColor(EDGE);
        for (int joint = x + 28; joint < x + width - 10; joint += 32)
            g.drawLine(joint, y + 3, joint, y + 6);
    }
}
