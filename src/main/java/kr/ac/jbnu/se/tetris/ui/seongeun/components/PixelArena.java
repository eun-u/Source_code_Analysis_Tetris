package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

/** 체력 변화에 반응하는 상단 픽셀 전투 무대. */
public final class PixelArena extends JComponent {
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
        if (this.playerName.equals(player) && hp < playerHp) playerHitAt = now;
        if (this.monsterName.equals(monster) && enemyHp < monsterHp) monsterHitAt = now;
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
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(13, 21, 40)); g.fillRect(0, 0, w, h);
            g.setColor(new Color(19, 36, 64)); g.fillRect(0, h / 3, w, h / 2);
            g.setColor(new Color(34, 61, 85)); g.fillRect(0, h * 3 / 4, w, h / 4);
            g.setColor(new Color(57, 82, 101));
            for (int i = 0; i < w; i += 42) g.fillRect(i, h * 3 / 4 + (i / 42 % 2) * 6, 32, 8);
            // 배경의 픽셀 별과 양쪽 발판
            g.setColor(new Color(108, 156, 181));
            for (int i = 17; i < w; i += 73) g.fillRect(i, 22 + i % 43, 3, 3);
            int scale = Math.max(2, Math.min(6, h < 180 ? h / 40 : h / 31));
            int playerX = w / 4 - 9 * scale, enemyX = w * 3 / 4 - 9 * scale;
            int spriteY = h * 3 / 4 - 13 * scale;
            drawFighter(g, playerX, spriteY, scale, false, recent(playerHitAt));
            drawFighter(g, enemyX, spriteY, scale, true, recent(monsterHitAt));
            int hpTop = h < 180 ? 0 : 13;
            drawHp(g, 18, hpTop, Math.max(100, w / 3), playerName, playerHp, playerMax, new Color(116, 221, 169));
            drawHp(g, w - Math.max(100, w / 3) - 18, hpTop, Math.max(100, w / 3),
                    monsterName, monsterHp, monsterMax, new Color(255, 135, 122));
            drawImpact(g, w, h, scale);
            g.setColor(new Color(136, 171, 193));
            g.drawRect(1, 1, w - 3, h - 3);
        } finally { g.dispose(); }
    }

    private void drawHp(Graphics2D g, int x, int y, int width, String name, int hp, int max, Color color) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        g.setColor(new Color(239, 242, 240)); g.drawString(name, x, y + 12);
        g.drawString(hp + " / " + max, x, y + 30);
        g.setColor(new Color(8, 13, 24)); g.fillRect(x - 2, y + 36, width + 4, 14);
        g.setColor(color.darker()); g.fillRect(x, y + 38, width, 10);
        g.setColor(color); g.fillRect(x, y + 38, Math.min(width, (int) ((long) width * hp / max)), 10);
    }

    private static boolean recent(long time) { return System.currentTimeMillis() - time < 330; }

    private void drawImpact(Graphics2D g, int w, int h, int scale) {
        long now = System.currentTimeMillis();
        long time = Math.max(playerHitAt, monsterHitAt);
        if (now - time >= 500) return;
        boolean hitPlayer = playerHitAt > monsterHitAt;
        float phase = (now - time) / 500f;
        int x = (int) ((hitPlayer ? .69 - .46 * phase : .31 + .46 * phase) * w);
        int y = h / 2 + (int) (Math.sin(phase * Math.PI) * -h / 9);
        g.setColor(hitPlayer ? new Color(255, 138, 97) : new Color(122, 234, 237));
        g.fillRect(x, y, scale * 3, scale * 3);
        g.fillRect(x + scale, y - scale, scale, scale * 5);
        g.fillRect(x - scale, y + scale, scale * 5, scale);
    }

    private void drawFighter(Graphics2D g, int x, int y, int size, boolean monster, boolean hit) {
        String[] rows = monster ? new String[] {
                "....KKKK....", "..KKRRRRKK..", ".KRRRRRRRRK.", "KRRW R W RRK",
                "KRRRRRRRRRRK", "KRRKKRRKKRRK", ".KRRRRRRRRK.", "..KRRRRRRK..",
                "...KRRRRK...", "..KKRRRRKK..", ".K..K..K..K.", "K...K..K...K"
        } : new String[] {
                "....KKKK....", "...KHHHHK...", "..KHHHHHHK..", "..KSKSSSKK..",
                "..KSSSSSSK..", "...KSSSSK...", "..KKBBBBKK..", ".KBBBBBBBBK.",
                ".KBBBBBBBBK.", "..KBBBBBBK..", "..K..KK..K..", ".KK..KK..KK."
        };
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row].replace(" ", "R");
            for (int col = 0; col < line.length(); col++) {
                char pixel = line.charAt(col);
                if (pixel == '.') continue;
                Color color;
                if (hit) color = Color.WHITE;
                else if (pixel == 'K') color = new Color(12, 21, 37);
                else if (pixel == 'W') color = new Color(255, 249, 219);
                else if (pixel == 'R') color = new Color(189, 94, 145);
                else if (pixel == 'H') color = new Color(59, 74, 118);
                else if (pixel == 'S') color = new Color(244, 199, 153);
                else color = new Color(87, 172, 222);
                g.setColor(color);
                g.fillRect(x + col * size, y + row * size, size, size);
            }
        }
    }
}
