package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import kr.ac.jbnu.se.tetris.story.MonsterTier;

/** 실제 전투 상태와 연결된 상단 퍼즐 RPG 무대. */
public final class PixelArena extends JComponent {
    public enum Effect {
        ATTACK, MONSTER_ATTACK, DAMAGE, HEAL, ITEM_ACQUIRE, ITEM_USE,
        LINE_CLEAR, FEVER, VICTORY, DEFEAT
    }

    private static final Color GOLD = UniversityPixelTheme.GOLD;
    private static final Color MINT = UniversityPixelTheme.MINT;
    private static final Color CORAL = UniversityPixelTheme.CORAL;
    private static final BufferedImage PLAYER_ART = readArt("/ui/university/avatar_player.png");
    private static final BufferedImage FALLBACK_MONSTER = readArt("/ui/university/avatar_professor.png");
    private static final BufferedImage HP_ICON = readArt("/ui/university/stat_stamina.png");
    private static final Map<String, BufferedImage> ART_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> ART_LOADING = ConcurrentHashMap.newKeySet();
    private static final int EFFECT_LIMIT = 12;
    private final ArrayDeque<Visual> visuals = new ArrayDeque<>();
    private final Timer animation;
    private int playerHp = 100, playerMax = 100, monsterHp = 100, monsterMax = 100;
    private String playerName = "PLAYER", monsterName = "MONSTER";
    private String monsterArtPath;
    private String chapter = "university";
    private boolean online;
    private MonsterTier pattern = MonsterTier.NORMAL;
    private long playerHitAt, monsterHitAt, playerAttackAt, monsterAttackAt;
    private boolean effectsEnabled = true;
    private boolean initialized;
    private int serial;

    public PixelArena() {
        setPreferredSize(new Dimension(850, 225));
        setMinimumSize(new Dimension(500, 165));
        animation = new Timer(50, event -> {
            expireVisuals();
            repaint();
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (effectsEnabled && isShowing()) animation.start();
                else animation.stop();
            }
        });
    }

    @Override public void addNotify() {
        super.addNotify();
        if (effectsEnabled && isShowing()) animation.start();
    }

    @Override public void removeNotify() {
        animation.stop();
        super.removeNotify();
    }

    /** 몬스터의 행동 패턴과 무대 색을 맞춘다. */
    public void setMonsterPattern(MonsterTier pattern) {
        this.pattern = pattern == null ? MonsterTier.NORMAL : pattern;
        repaint();
    }

    /** 상대 아틀라스 키(예: university:0) 또는 /ui/puzzle-rpg/ 경로. */
    public void setMonsterArt(String resourcePath) {
        monsterArtPath = resourcePath;
        if (resourcePath != null && !ART_CACHE.containsKey(resourcePath)
                && ART_LOADING.add(resourcePath)) {
            Thread loader = new Thread(() -> {
                BufferedImage image = resourcePath.startsWith("/ui/puzzle-rpg/")
                        ? readArt(resourcePath) : GameArt.sprite(resourcePath);
                if (image != null) ART_CACHE.put(resourcePath, image);
                ART_LOADING.remove(resourcePath);
                SwingUtilities.invokeLater(this::repaint);
            }, "tetris-arena-art");
            loader.setDaemon(true);
            loader.start();
        }
        repaint();
    }

    public void setOnline(boolean online) {
        this.online = online;
        repaint();
    }

    /** university, graduation, employment에 맞춰 배경 무대를 바꾼다. */
    public void setChapter(String chapter) {
        if ("graduation".equals(chapter) || "employment".equals(chapter))
            this.chapter = chapter;
        else this.chapter = "university";
        repaint();
    }

    /** 새 전투의 첫 스냅샷을 기준점으로 삼아 이전 상대의 피격 연출을 남기지 않는다. */
    public void resetForEncounter() {
        visuals.clear();
        initialized = false;
        playerName = "PLAYER";
        monsterName = "MONSTER";
        playerHp = playerMax = monsterHp = monsterMax = 100;
        playerHitAt = monsterHitAt = playerAttackAt = monsterAttackAt = 0;
        repaint();
    }

    /** 저사양 환경에서 움직임을 멈추고 현재 상태만 그린다. */
    public void setEffectsEnabled(boolean enabled) {
        effectsEnabled = enabled;
        if (enabled && isShowing()) animation.start();
        else { animation.stop(); visuals.clear(); }
        repaint();
    }

    public void showEffect(Effect effect, int amount) {
        showEffect(effect, amount, false);
    }

    /** targetMonster는 몬스터 측 회복/아이템 효과에도 사용한다. */
    public void showEffect(Effect effect, int amount, boolean targetMonster) {
        if (effect == null || !effectsEnabled) return;
        long now = System.currentTimeMillis();
        if (visuals.size() >= EFFECT_LIMIT) visuals.removeFirst();
        visuals.addLast(new Visual(effect, Math.max(0, amount), targetMonster, now, serial++));
        if (effect == Effect.ATTACK) playerAttackAt = now;
        if (effect == Effect.MONSTER_ATTACK) monsterAttackAt = now;
        if (effect == Effect.DAMAGE) {
            if (targetMonster) monsterHitAt = now;
            else playerHitAt = now;
        }
        if (isShowing()) animation.start();
        repaint();
    }

    public void showItemEffect(String itemId) { showItemEffect(itemId, false); }

    /** 아이템마다 식별 가능한 연출을 준다. targetMonster는 효과를 받는 쪽이다. */
    public void showItemEffect(String itemId, boolean targetMonster) {
        if (itemId == null || !effectsEnabled) return;
        long now = System.currentTimeMillis();
        if (visuals.size() >= EFFECT_LIMIT) visuals.removeFirst();
        visuals.addLast(new Visual(Effect.ITEM_USE, 0, targetMonster, now, serial++, itemId));
        if (isShowing()) animation.start();
        repaint();
    }

    public void update(String player, int hp, int maxHp, String monster, int enemyHp, int enemyMax) {
        String nextPlayer = player == null ? "PLAYER" : player;
        String nextMonster = monster == null ? "MONSTER" : monster;
        if (initialized && playerName.equals(nextPlayer)) {
            if (hp < playerHp) {
                monsterAttackAt = System.currentTimeMillis();
                showEffect(Effect.DAMAGE, playerHp - hp, false);
            }
            else if (hp > playerHp) showEffect(Effect.HEAL, hp - playerHp, false);
        }
        if (initialized && monsterName.equals(nextMonster)) {
            if (enemyHp < monsterHp) {
                playerAttackAt = System.currentTimeMillis();
                showEffect(Effect.DAMAGE, monsterHp - enemyHp, true);
            }
            else if (enemyHp > monsterHp) showEffect(Effect.HEAL, enemyHp - monsterHp, true);
        }
        playerName = nextPlayer;
        monsterName = nextMonster;
        playerHp = Math.max(0, hp);
        playerMax = Math.max(1, maxHp);
        monsterHp = Math.max(0, enemyHp);
        monsterMax = Math.max(1, enemyMax);
        initialized = true;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            long now = System.currentTimeMillis();
            paintBackdrop(g, w, h, now);
            int artHeight = Math.min(126, Math.max(58, h - 82));
            int artWidth = artHeight * 6 / 7;
            int artY = h - 16 - artHeight;
            int idle = effectsEnabled ? (int) (2 * Math.sin(now / 210.0)) : 0;
            int localLunge = lunge(now - playerAttackAt, 1);
            int enemyLunge = lunge(now - monsterAttackAt, -1);
            paintAvatar(g, PLAYER_ART, w / 4 - artWidth / 2 + localLunge, artY + idle,
                    artWidth, artHeight, now - playerHitAt < 310, playerHp <= 0, false);
            BufferedImage monsterArt = online ? PLAYER_ART :
                    monsterArtPath == null ? null : ART_CACHE.get(monsterArtPath);
            paintAvatar(g, monsterArt == null ? FALLBACK_MONSTER : monsterArt,
                    w * 3 / 4 - artWidth / 2 + enemyLunge, artY - idle,
                    artWidth, artHeight, now - monsterHitAt < 310, monsterHp <= 0, true);

            paintVisuals(g, w, h, now);
            int barWidth = Math.min(240, Math.max(100, w / 3));
            paintHp(g, 14, 9, barWidth, playerName, playerHp, playerMax, MINT);
            paintHp(g, w - barWidth - 14, 9, barWidth,
                    monsterName, monsterHp, monsterMax, CORAL);
            g.setColor(UniversityPixelTheme.BLACK);
            g.drawRect(1, 1, w - 3, h - 3);
            g.drawRect(2, 2, w - 5, h - 5);
            g.setColor(UniversityPixelTheme.LINE);
            g.drawRect(5, 5, w - 11, h - 11);
        } finally { g.dispose(); }
    }

    private void paintBackdrop(Graphics2D g, int w, int h, long now) {
        g.setColor(UniversityPixelTheme.BG);
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x10103A));
        g.fillRect(7, 7, w - 14, h - 14);
        g.setColor(new Color(0x171044));
        for (int x = 11; x < w - 8; x += 18) g.fillRect(x, 9, 1, h - 18);
        for (int y = 11; y < h - 8; y += 18) g.fillRect(9, y, w - 18, 1);
        paintChapterBackdrop(g, w, h);
        // 공연장 조명과 발판은 반복무늬 대신 전투 대상의 위치를 보여준다.
        Color accent = pattern == MonsterTier.BOSS ? CORAL :
                pattern == MonsterTier.ELITE ? GOLD : MINT;
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 22));
        g.fillPolygon(new int[] { w / 4 - 19, w / 4 + 19, w / 4 + 96, w / 4 - 96 },
                new int[] { 64, 64, h - 21, h - 21 }, 4);
        g.fillPolygon(new int[] { w * 3 / 4 - 19, w * 3 / 4 + 19,
                w * 3 / 4 + 96, w * 3 / 4 - 96 },
                new int[] { 64, 64, h - 21, h - 21 }, 4);
        g.setColor(UniversityPixelTheme.PANEL);
        g.fillRect(8, h - 19, w - 16, 11);
        g.setColor(UniversityPixelTheme.LINE);
        g.fillRect(8, h - 20, w - 16, 3);
        g.setColor(accent);
        for (int x = 31; x < w - 24; x += 91) {
            int twinkle = effectsEnabled && (now / 300 + x) % 3 == 0 ? 5 : 3;
            g.fillRect(x, 69 + x % 17, twinkle, twinkle);
        }
        g.setFont(UniversityPixelTheme.font(h < 185 ? 12 : 15, Font.BOLD));
        String vs = "VS";
        FontMetrics metrics = g.getFontMetrics();
        int vx = (w - metrics.stringWidth(vs)) / 2;
        g.setColor(UniversityPixelTheme.BLACK);
        g.fillRect(vx - 10, Math.max(68, h / 2) - 19, metrics.stringWidth(vs) + 20, 29);
        g.setColor(accent);
        g.drawString(vs, vx, Math.max(68, h / 2));
    }

    private void paintChapterBackdrop(Graphics2D g, int w, int h) {
        int baseline = h - 22;
        if ("graduation".equals(chapter)) {
            g.setColor(new Color(0x2F234F));
            g.fillRect(w / 2 - 59, baseline - 44, 118, 36);
            g.setColor(new Color(0x695381));
            g.fillRect(w / 2 - 61, baseline - 47, 122, 4);
            g.fillRect(w / 2 - 49, baseline - 9, 5, 11);
            g.fillRect(w / 2 + 44, baseline - 9, 5, 11);
            g.setColor(new Color(0xBCA0B6));
            g.fillRect(w / 2 - 32, baseline - 52, 20, 5);
            g.fillRect(w / 2 + 10, baseline - 52, 26, 5);
        } else if ("employment".equals(chapter)) {
            g.setColor(new Color(0x293361));
            for (int x = 30; x < w - 25; x += 76) {
                int top = 78 + x % 27;
                g.fillRect(x, top, 43, baseline - top);
                g.setColor(new Color(0x73629A));
                for (int y = top + 8; y < baseline - 4; y += 14) {
                    g.fillRect(x + 7, y, 5, 4);
                    g.fillRect(x + 23, y, 5, 4);
                }
                g.setColor(new Color(0x293361));
            }
        } else {
            g.setColor(new Color(0x253558));
            g.fillRect(w / 2 - 75, 77, 150, Math.max(24, baseline - 94));
            g.setColor(new Color(0x5D577B));
            g.fillRect(w / 2 - 78, 75, 156, 4);
            g.setColor(new Color(0x65B6A5));
            g.fillRect(w / 2 - 51, 94, 77, 2);
            g.fillRect(w / 2 - 51, 104, 55, 2);
        }
    }

    private static void paintAvatar(Graphics2D g, BufferedImage image, int x, int y,
                                    int width, int height, boolean hit, boolean defeated,
                                    boolean monster) {
        g.setColor(new Color(0, 0, 0, 92));
        g.fillOval(x + 4, y + height - 9, width - 3, 15);
        g.setColor(UniversityPixelTheme.BLACK);
        g.fillRect(x + 5, y + 5, width, height);
        Composite prior = g.getComposite();
        if (defeated) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        if (image != null) g.drawImage(image, x, y + (defeated ? 9 : 0), width, height, null);
        else {
            g.setColor(monster ? CORAL : MINT);
            g.fillRect(x, y, width, height);
            g.setColor(UniversityPixelTheme.BLACK);
            g.fillRect(x + width / 4, y + height / 3, width / 2, height / 4);
        }
        g.setComposite(prior);
        Color trim = monster ? CORAL : MINT;
        g.setColor(new Color(trim.getRed(), trim.getGreen(), trim.getBlue(), 110));
        g.drawRect(x - 2, y - 2, width + 3, height + 3);
        g.fillRect(x - 3, y - 3, 13, 3);
        g.fillRect(x + width - 8, y + height, 13, 3);
        if (hit) {
            g.setColor(new Color(255, 240, 228, 136));
            g.fillRect(x, y, width, height);
            g.setColor(CORAL);
            g.fillRect(x - 3, y + height / 4, 4, height / 2);
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
        g.drawString(fit(name, metrics, width - 39), x + 29, y + 20);
        g.setColor(UniversityPixelTheme.TEXT_SUB);
        g.drawString(hp + " / " + max, x + 8, y + 36);
        int meterX = x + 8, meterY = y + 41, meterWidth = width - 16;
        g.setColor(UniversityPixelTheme.BLACK);
        g.fillRect(meterX, meterY, meterWidth, 8);
        g.setColor(fill);
        int filled = Math.min(meterWidth - 2, (int) ((long) (meterWidth - 2) * hp / max));
        g.fillRect(meterX + 1, meterY + 1, Math.max(0, filled), 6);
        g.setColor(UniversityPixelTheme.BLACK);
        for (int tick = meterX + 10; tick < meterX + meterWidth - 1; tick += 11)
            g.fillRect(tick, meterY + 1, 1, 6);
    }

    private void paintVisuals(Graphics2D g, int w, int h, long now) {
        for (Visual visual : visuals) {
            long age = now - visual.started;
            if (age < 0 || age > duration(visual.effect)) continue;
            float progress = Math.min(1f, age / (float) duration(visual.effect));
            Color color = colorFor(visual);
            int targetX = (visual.targetMonster ? w * 3 / 4 : w / 4);
            int centerY = Math.max(104, h / 2 + 11);
            switch (visual.effect) {
                case ATTACK:
                case MONSTER_ATTACK:
                    int sourceX = visual.effect == Effect.ATTACK ? w / 4 : w * 3 / 4;
                    int destinationX = visual.effect == Effect.ATTACK ? w * 3 / 4 : w / 4;
                    paintProjectile(g, sourceX, destinationX, centerY, progress, color);
                    paintBurst(g, destinationX, centerY, progress, visual.serial, color, 11);
                    break;
                case LINE_CLEAR:
                    int sweepX = (int) ((w + 80) * progress) - 40;
                    g.setColor(new Color(165, 253, 243, 90));
                    g.fillRect(sweepX - 65, h - 65, 65, 22);
                    g.setColor(color);
                    g.fillRect(sweepX - 3, h - 72, 5, 38);
                    paintBurst(g, sweepX, h - 54, progress, visual.serial, color, 7);
                    break;
                case ITEM_USE:
                    paintItemEffect(g, w, h, targetX, centerY, progress, visual, color);
                    break;
                case VICTORY:
                    paintVictory(g, w, h, progress);
                    break;
                case DEFEAT:
                    g.setColor(new Color(18, 8, 30, (int) (Math.sin(progress * Math.PI) * 150)));
                    g.fillRect(8, 63, w - 16, Math.max(0, h - 82));
                    break;
                default:
                    paintBurst(g, targetX, centerY, progress, visual.serial, color, 13);
                    break;
            }
            if (age > 130 && visual.effect != Effect.ATTACK && visual.effect != Effect.MONSTER_ATTACK)
                paintFloatingLabel(g, visual, targetX, centerY, progress, color);
        }
    }

    private static void paintProjectile(Graphics2D g, int fromX, int toX, int y,
                                        float progress, Color color) {
        float move = Math.min(1f, progress * 1.6f);
        int x = Math.round(fromX + (toX - fromX) * move);
        int direction = fromX < toX ? 1 : -1;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g.fillRect(x - direction * 28 - 9, y - 5, 32, 14);
        g.setColor(color);
        g.fillRect(x - 7, y - 7, 14, 14);
        g.fillRect(x - 2, y - 12, 4, 24);
        g.fillRect(x - 12, y - 2, 24, 4);
    }

    private static void paintBurst(Graphics2D g, int x, int y, float progress,
                                   int serial, Color color, int count) {
        int opacity = Math.max(0, Math.round(255 * (1f - progress)));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
        for (int index = 0; index < count; index++) {
            double angle = (index * 6.28318 / count) + serial * .27;
            int distance = 8 + Math.round(55 * progress * (0.64f + (index % 4) * .13f));
            int px = x + (int) (Math.cos(angle) * distance);
            int py = y + (int) (Math.sin(angle) * distance);
            int size = Math.max(2, 7 - Math.round(progress * 5));
            g.fillRect(px, py, size, size);
            if (index % 3 == 0) g.fillRect(px + 2, py - 4, 2, size + 8);
        }
        if (progress < .45f) {
            int radius = 10 + Math.round(progress * 75);
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    private static void paintVictory(Graphics2D g, int w, int h, float progress) {
        Color gold = GOLD;
        paintBurst(g, w / 2, h / 2, progress, 2, gold, 22);
        g.setColor(new Color(gold.getRed(), gold.getGreen(), gold.getBlue(),
                Math.max(0, (int) (150 * (1f - progress)))));
        for (int i = 0; i < 7; i++) {
            int x = (int) ((i + .5) * w / 7);
            int y = (int) ((i % 2 == 0 ? .38 : .58) * h);
            g.fillRect(x, y - Math.round(28 * progress), 5, 13);
        }
    }

    private static void paintItemEffect(Graphics2D g, int w, int h, int x, int y,
                                        float progress, Visual visual, Color color) {
        String id = visual.itemId;
        if ("heal".equals(id)) {
            g.setColor(color);
            for (int i = 0; i < 6; i++) {
                int crossX = x - 27 + i * 10;
                int crossY = y + 27 - Math.round(55 * progress) - (i % 2) * 11;
                g.fillRect(crossX + 3, crossY, 4, 14);
                g.fillRect(crossX - 2, crossY + 5, 14, 4);
            }
        } else if ("shield".equals(id)) {
            int radius = 35 + Math.round(18 * progress);
            g.setColor(color);
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            g.drawOval(x - radius + 5, y - radius + 5, (radius - 5) * 2, (radius - 5) * 2);
            for (int i = -1; i <= 1; i++) g.fillRect(x + i * 14 - 2, y - radius - 3, 5, 7);
        } else if ("damage_boost".equals(id)) {
            g.setColor(color);
            int direction = visual.targetMonster ? -1 : 1;
            for (int i = 0; i < 3; i++) {
                int baseX = x + direction * (10 + i * 18 + Math.round(progress * 18));
                g.fillPolygon(new int[] { baseX, baseX - direction * 12, baseX - direction * 12 },
                        new int[] { y, y - 12, y + 12 }, 3);
            }
        } else if ("garbage_bomb".equals(id)) {
            paintBurst(g, x, y, progress, visual.serial, color, 22);
            g.setColor(color);
            int radius = 9 + Math.round(progress * 50);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int outerX = x + (int) (Math.cos(angle) * (radius + 13));
                int outerY = y + (int) (Math.sin(angle) * (radius + 13));
                int innerX = x + (int) (Math.cos(angle) * radius);
                int innerY = y + (int) (Math.sin(angle) * radius);
                g.drawLine(innerX, innerY, outerX, outerY);
            }
        } else if ("line_cleaner".equals(id)) {
            int sweep = (int) ((w + 90) * progress) - 45;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 72));
            g.fillRect(sweep - 70, h - 62, 70, 22);
            g.setColor(color);
            g.fillRect(sweep - 2, h - 72, 6, 41);
            for (int i = 0; i < 5; i++)
                g.fillRect(sweep - 25 + i * 9, h - 36 - (i % 2) * 6, 3, 3);
        } else if ("fever_charge".equals(id)) {
            paintBurst(g, x, y, progress, visual.serial, color, 17);
            g.setColor(color);
            int rays = 9 + Math.round(progress * 17);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4 + progress * 2;
                g.fillRect(x + (int) (Math.cos(angle) * rays),
                        y + (int) (Math.sin(angle) * rays), 5, 5);
            }
        } else if ("time_warp".equals(id)) {
            g.setColor(color);
            int radius = 30 + Math.round(progress * 16);
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            g.drawOval(x - radius + 7, y - radius + 7, (radius - 7) * 2, (radius - 7) * 2);
            g.drawLine(x, y, x, y - radius + 11);
            g.drawLine(x, y, x + radius / 2, y + radius / 3);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                g.fillRect(x + (int) (Math.cos(angle) * (radius - 3)),
                        y + (int) (Math.sin(angle) * (radius - 3)), 3, 3);
            }
        } else if ("nullify".equals(id)) {
            g.setColor(color);
            int radius = 18 + Math.round(34 * progress);
            g.drawLine(x - radius, y - radius, x + radius, y + radius);
            g.drawLine(x + radius, y - radius, x - radius, y + radius);
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        } else {
            paintBurst(g, x, y, progress, visual.serial, color, 10);
        }
    }

    private static void paintFloatingLabel(Graphics2D g, Visual visual, int x, int y,
                                           float progress, Color color) {
        String label;
        switch (visual.effect) {
            case DAMAGE: label = "-" + visual.amount; break;
            case HEAL: label = "+" + visual.amount; break;
            case ITEM_ACQUIRE: label = "ITEM GET"; break;
            case ITEM_USE: label = itemLabel(visual.itemId); break;
            case LINE_CLEAR: label = visual.amount + " LINE"; break;
            case FEVER: label = "FEVER!"; break;
            case VICTORY: label = "CLEAR!"; break;
            case DEFEAT: label = "GAME OVER"; break;
            default: return;
        }
        g.setFont(UniversityPixelTheme.font(visual.effect == Effect.VICTORY ? 21 : 16, Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        int labelX = x - metrics.stringWidth(label) / 2;
        int labelY = Math.max(84, y - 28 - Math.round(25 * progress));
        g.setColor(UniversityPixelTheme.BLACK);
        g.drawString(label, labelX + 2, labelY + 2);
        g.setColor(color);
        g.drawString(label, labelX, labelY);
    }

    private static String itemLabel(String id) {
        if ("heal".equals(id)) return "회복";
        if ("shield".equals(id)) return "보호막";
        if ("damage_boost".equals(id)) return "공격 증폭";
        if ("garbage_bomb".equals(id)) return "가비지 폭탄";
        if ("line_cleaner".equals(id)) return "줄 정리";
        if ("fever_charge".equals(id)) return "피버 충전";
        if ("time_warp".equals(id)) return "시간 왜곡";
        if ("nullify".equals(id)) return "무효화";
        return "ITEM!";
    }

    private static Color colorFor(Visual visual) {
        if (visual.effect == Effect.ITEM_USE) {
            String id = visual.itemId;
            if ("garbage_bomb".equals(id)) return CORAL;
            if ("time_warp".equals(id)) return new Color(0x7BBEFF);
            if ("nullify".equals(id)) return new Color(0xAD85EC);
            if ("fever_charge".equals(id)) return new Color(0xFF92E2);
            if ("damage_boost".equals(id)) return GOLD;
            return MINT;
        }
        switch (visual.effect) {
            case DAMAGE: case MONSTER_ATTACK: case DEFEAT: return CORAL;
            case ITEM_ACQUIRE: case VICTORY: case LINE_CLEAR: return GOLD;
            case FEVER: return new Color(0xFF92E2);
            default: return MINT;
        }
    }

    private static int duration(Effect effect) {
        return effect == Effect.VICTORY || effect == Effect.DEFEAT ? 1250 : 850;
    }

    private static int lunge(long elapsed, int direction) {
        if (elapsed < 0 || elapsed >= 250) return 0;
        float phase = elapsed / 250f;
        return Math.round(direction * 22 * (float) Math.sin(Math.PI * phase));
    }

    private void expireVisuals() {
        long now = System.currentTimeMillis();
        while (!visuals.isEmpty() && now - visuals.peekFirst().started >
                duration(visuals.peekFirst().effect)) visuals.removeFirst();
    }

    private static String fit(String text, FontMetrics metrics, int maxWidth) {
        String value = text == null ? "" : text;
        if (metrics.stringWidth(value) <= maxWidth) return value;
        while (value.length() > 1 && metrics.stringWidth(value + "…") > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    private static BufferedImage readArt(String path) {
        try (InputStream stream = PixelArena.class.getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException exception) { return null; }
    }

    private static final class Visual {
        final Effect effect;
        final int amount;
        final boolean targetMonster;
        final long started;
        final int serial;
        final String itemId;
        Visual(Effect effect, int amount, boolean targetMonster, long started, int serial) {
            this(effect, amount, targetMonster, started, serial, null);
        }
        Visual(Effect effect, int amount, boolean targetMonster, long started,
               int serial, String itemId) {
            this.effect = effect;
            this.amount = amount;
            this.targetMonster = targetMonster;
            this.started = started;
            this.serial = serial;
            this.itemId = itemId;
        }
    }
}
