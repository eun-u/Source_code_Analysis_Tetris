package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

/** University_Simulation 에셋을 임시로 쓰는 상단 전투 무대. HP 변화와 피격 연출은 실제 전투 상태에 연동된다. */
public final class PixelArena extends JComponent {
    private static final BufferedImage PLAYER_ART = loadArt("avatar_player.png");
    private static final BufferedImage MONSTER_ART = loadArt("avatar_professor.png");
    private static final BufferedImage HP_ICON = loadArt("stat_stamina.png");
    private int playerHp = 100, playerMax = 100, monsterHp = 100, monsterMax = 100;
    private String playerName = "PLAYER", monsterName = "MONSTER";
    private long playerHitAt, monsterHitAt;
    private final Timer animation;

    public PixelArena() {
        setPreferredSize(new Dimension(850, 225));
        setMinimumSize(new Dimension(500, 165));
        animation = new Timer(40, null);
        animation.addActionListener(event -> {
            repaint();
            if (System.currentTimeMillis() - Math.max(playerHitAt, monsterHitAt) > 520) animation.stop();
        });
    }

    public void update(String player, int hp, int maxHp, String monster, int enemyHp, int enemyMax) {
        long now = System.currentTimeMillis();
        if (playerName.equals(player) && hp < playerHp) playerHitAt = now;
        if (monsterName.equals(monster) && enemyHp < monsterHp) monsterHitAt = now;
        playerName = player == null ? "PLAYER" : player;
        monsterName = monster == null ? "MONSTER" : monster;
        playerHp = Math.max(0, hp);
        playerMax = Math.max(1, maxHp);
        monsterHp = Math.max(0, enemyHp);
        monsterMax = Math.max(1, enemyMax);
        if (now - Math.max(playerHitAt, monsterHitAt) < 520) animation.start();
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            paintBackdrop(g, w, h);

            int artHeight = Math.min(126, Math.max(58, h - 82));
            int artWidth = artHeight * 6 / 7;
            int artY = h - 16 - artHeight;
            paintAvatar(g, PLAYER_ART, w / 4 - artWidth / 2, artY, artWidth, artHeight, recent(playerHitAt), false);
            paintAvatar(g, MONSTER_ART, w * 3 / 4 - artWidth / 2, artY, artWidth, artHeight, recent(monsterHitAt), true);

            int barWidth = Math.min(240, Math.max(100, w / 3));
            paintHp(g, 14, 9, barWidth, playerName, playerHp, playerMax, UniversityPixelTheme.MINT);
            paintHp(g, w - barWidth - 14, 9, barWidth,
                    monsterName, monsterHp, monsterMax, UniversityPixelTheme.CORAL);
            paintImpact(g, w, h);
            g.setColor(UniversityPixelTheme.BLACK);
            g.drawRect(1, 1, w - 3, h - 3);
            g.drawRect(2, 2, w - 5, h - 5);
            g.setColor(UniversityPixelTheme.LINE);
            g.drawRect(5, 5, w - 11, h - 11);
        } finally { g.dispose(); }
    }

    private static void paintBackdrop(Graphics2D g, int w, int h) {
        g.setColor(UniversityPixelTheme.BG);
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x10103A));
        g.fillRect(7, 7, w - 14, h - 14);
        g.setColor(new Color(0x171044));
        for (int x = 11; x < w - 8; x += 18) g.fillRect(x, 9, 1, h - 18);
        for (int y = 11; y < h - 8; y += 18) g.fillRect(9, y, w - 18, 1);
        g.setColor(UniversityPixelTheme.PANEL);
        g.fillRect(8, h - 19, w - 16, 11);
        g.setColor(UniversityPixelTheme.LINE);
        g.fillRect(8, h - 20, w - 16, 3);
        g.setColor(UniversityPixelTheme.GOLD);
        for (int x = 31; x < w - 24; x += 91) g.fillRect(x, 67 + x % 17, 3, 3);
        g.setFont(UniversityPixelTheme.font(h < 185 ? 12 : 15, Font.BOLD));
        String vs = "VS";
        FontMetrics metrics = g.getFontMetrics();
        int vx = (w - metrics.stringWidth(vs)) / 2;
        g.setColor(UniversityPixelTheme.BLACK);
        g.fillRect(vx - 10, Math.max(68, h / 2) - 19, metrics.stringWidth(vs) + 20, 29);
        g.setColor(UniversityPixelTheme.GOLD);
        g.drawString(vs, vx, Math.max(68, h / 2));
    }

    private static void paintAvatar(Graphics2D g, BufferedImage image, int x, int y,
                                    int width, int height, boolean hit, boolean monster) {
        g.setColor(UniversityPixelTheme.BLACK);
        g.fillRect(x + 5, y + 5, width, height);
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            // 리소스가 빠진 개발용 실행에서도 전투 대상을 볼 수 있게 둔다.
            g.setColor(monster ? UniversityPixelTheme.CORAL : UniversityPixelTheme.MINT);
            g.fillRect(x, y, width, height);
            g.setColor(UniversityPixelTheme.BLACK);
            g.fillRect(x + width / 4, y + height / 3, width / 2, height / 4);
        }
        if (hit) {
            g.setColor(new Color(255, 247, 232, 130));
            g.fillRect(x, y, width, height);
        }
    }

    private static void paintHp(Graphics2D g, int x, int y, int width,
                                String name, int hp, int max, Color fill) {
        g.setColor(UniversityPixelTheme.PANEL);
        g.fillRect(x, y, width, 54);
        g.setColor(UniversityPixelTheme.BLACK);
        g.drawRect(x, y, width - 1, 53);
        g.setColor(UniversityPixelTheme.LINE);
        g.drawRect(x + 3, y + 3, width - 7, 47);
        if (HP_ICON != null) g.drawImage(HP_ICON, x + 7, y + 6, 19, 19, null);
        g.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        g.setColor(UniversityPixelTheme.TEXT);
        FontMetrics metrics = g.getFontMetrics();
        String visibleName = fit(name, metrics, width - 39);
        g.drawString(visibleName, x + 29, y + 20);
        String amount = hp + " / " + max;
        g.setColor(UniversityPixelTheme.TEXT_SUB);
        g.drawString(amount, x + 8, y + 36);
        int meterX = x + 8, meterY = y + 41, meterWidth = width - 16;
        g.setColor(UniversityPixelTheme.BLACK);
        g.fillRect(meterX, meterY, meterWidth, 8);
        g.setColor(fill);
        int filled = Math.min(meterWidth - 2, (int) ((long) (meterWidth - 2) * hp / max));
        g.fillRect(meterX + 1, meterY + 1, Math.max(0, filled), 6);
        g.setColor(UniversityPixelTheme.BLACK);
        for (int tick = meterX + 10; tick < meterX + meterWidth - 1; tick += 11) {
            g.fillRect(tick, meterY + 1, 1, 6);
        }
    }

    private void paintImpact(Graphics2D g, int width, int height) {
        long now = System.currentTimeMillis();
        long hitTime = Math.max(playerHitAt, monsterHitAt);
        if (now - hitTime >= 500) return;
        boolean hitPlayer = playerHitAt > monsterHitAt;
        float phase = (now - hitTime) / 500f;
        int x = (int) ((hitPlayer ? .69 - .46 * phase : .31 + .46 * phase) * width);
        int y = height / 2 + (int) (Math.sin(phase * Math.PI) * -height / 9);
        g.setColor(hitPlayer ? UniversityPixelTheme.CORAL : UniversityPixelTheme.MINT);
        g.fillRect(x, y, 18, 18);
        g.fillRect(x + 6, y - 6, 6, 30);
        g.fillRect(x - 6, y + 6, 30, 6);
    }

    private static String fit(String text, FontMetrics metrics, int maxWidth) {
        String value = text == null ? "" : text;
        if (metrics.stringWidth(value) <= maxWidth) return value;
        while (value.length() > 1 && metrics.stringWidth(value + "…") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "…";
    }

    private static boolean recent(long hitTime) {
        return System.currentTimeMillis() - hitTime < 330;
    }

    private static BufferedImage loadArt(String file) {
        try (InputStream stream = PixelArena.class.getResourceAsStream("/ui/university/" + file)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException exception) {
            return null;
        }
    }
}
