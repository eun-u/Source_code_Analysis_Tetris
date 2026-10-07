package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameArt;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameMenu;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/**
 * 게임 타이틀 화면. 화면 전체에 한국 캠퍼스 야경을 깔고, 왼쪽에 로고와 세로 메뉴,
 * 오른쪽 아래에 다음 스토리 전투 카드를 둔다. ↑↓·Enter로도 고를 수 있다.
 */
public class MainLobbyPanel extends JPanel {
    private static final BufferedImage CAMPUS = loadCampus();
    private static final String SIGNED_IN = "온라인 PvP 로그인 완료";
    private static final Color GOLD = UniversityPixelTheme.GOLD, MINT = UniversityPixelTheme.MINT;

    private final GameMenu menu = new GameMenu();
    private final GameMenu.Item storyButton = menu.add("스토리 시작", "9개의 전투로 첫 출근까지", GOLD);
    private final GameMenu.Item onlineButton = menu.add("온라인 대전", "계정으로 1:1 PvP", MINT);
    private final GameMenu.Item localButton = menu.add("로컬 모드", "무한 · 40줄 스프린트", MINT);
    private final GameMenu.Item tutorialButton = menu.add("튜토리얼", "다섯 동작을 직접 연습", MINT);
    private final GameMenu.Item characterButton = menu.add("캐릭터 / 상점", "코인으로 전투 스타일 선택", GOLD);
    private final GameMenu.Item serverButton = menu.add("PvP 랭킹", "공식 온라인 TOP 100", MINT);
    private final GameButton settingsButton = new GameButton("설정");
    private final JLabel account = UniversityPixelTheme.chip("로컬 플레이", UniversityPixelTheme.PANEL_LIGHT);
    private final JLabel coins = UniversityPixelTheme.chip("0 COINS", GOLD);
    private final Logo logo = new Logo();
    private final NextBattleCard nextCard = new NextBattleCard();
    private final JLabel keys = UniversityPixelTheme.label("↑ ↓  메뉴 선택     ENTER  결정     게임 중  ← → 이동 · ↑ ↓ 회전 · SPACE 낙하 · C HOLD",
            11, Font.PLAIN, UniversityPixelTheme.TEXT_SUB);
    private final Timer animation = new Timer(40, event -> repaint());

    public MainLobbyPanel() {
        setLayout(null);
        setBackground(UniversityPixelTheme.BG);
        storyButton.setName("lobbyStory");
        onlineButton.setName("lobbyOnline");
        localButton.setName("lobbyLocal");
        settingsButton.secondary();
        settingsButton.setPreferredSize(new Dimension(84, 34));
        settingsButton.setToolTipText("소리 · 온라인 계정 · 튜토리얼 · LAN 설정");
        account.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        coins.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        add(logo); add(menu); add(nextCard); add(account); add(coins); add(settingsButton); add(keys);
        nextCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { storyButton.doClick(); }
        });
        animation.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) { animation.start(); logo.shownAt = System.currentTimeMillis(); menu.select(0); }
                else animation.stop();
            }
        });
        setAccountStatus("로컬 플레이");
    }

    @Override public void doLayout() {
        int w = getWidth(), h = getHeight();
        int pad = UniversityPixelTheme.GUTTER;
        Dimension settings = settingsButton.getPreferredSize();
        settingsButton.setBounds(w - pad - settings.width, 16, settings.width, settings.height);
        Dimension accountSize = account.getPreferredSize(), coinSize = coins.getPreferredSize();
        account.setBounds(w - pad - settings.width - 10 - accountSize.width, 22, accountSize.width, accountSize.height);
        coins.setBounds(account.getX() - 8 - coinSize.width, 22, coinSize.width, coinSize.height);
        int logoHeight = Math.max(90, Math.min(130, h / 6));
        logo.setBounds(pad, 14, Math.min(520, w / 2), logoHeight);
        int menuWidth = Math.max(270, Math.min(360, w * 36 / 100));
        int itemHeight = Math.max(40, Math.min(54, (h - logoHeight - 90) / 6 - 4));
        int menuHeight = itemHeight * 6 + 4 * 5;
        menu.setBounds(pad, logo.getY() + logoHeight + 8, menuWidth, menuHeight);
        int cardWidth = Math.max(230, Math.min(330, w * 30 / 100));
        int cardHeight = Math.max(250, Math.min(380, h * 46 / 100));
        nextCard.setBounds(w - pad - cardWidth, h - 40 - cardHeight, cardWidth, cardHeight);
        keys.setBounds(pad, h - 28, w - pad * 2, 18);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x11212A), 0, h, new Color(0x10191D)));
            g.fillRect(0, 0, w, h);
            if (CAMPUS != null) {
                int artWidth = Math.max(w, CAMPUS.getWidth() * h / CAMPUS.getHeight());
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(CAMPUS, (w - artWidth) / 2, 0, artWidth, h, null);
            }
            g.setColor(new Color(8, 17, 21, 65));
            g.fillRect(0, 0, w, h);
            // 메뉴와 다음 전투 카드 사이에 풍경을 남기면서 글자를 읽기 쉽게 한다.
            g.setPaint(new GradientPaint(0, 0, new Color(8, 17, 21, 222), Math.round(w * 0.57f), 0, new Color(8, 17, 21, 10)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, h - 120, new Color(8, 17, 21, 0), 0, h, new Color(8, 17, 21, 220)));
            g.fillRect(0, h - 120, w, 120);
        } finally { g.dispose(); }
    }

    /** 블록이 떨어져 쌓이는 듯한 로고. 화면이 보일 때마다 짧게 등장 연출을 한다. */
    private static final class Logo extends JComponent {
        long shownAt;

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                long now = System.currentTimeMillis(), age = shownAt == 0 ? 1000 : now - shownAt;
                int h = getHeight();
                int big = Math.max(34, Math.min(58, h * 46 / 100));
                float drop = Math.min(1f, age / 380f);
                float ease = 1 - (1 - drop) * (1 - drop);
                int offset = Math.round((1 - ease) * -40);
                Color[] blocks = { new Color(104, 221, 235), GOLD, new Color(201, 139, 255), MINT, UniversityPixelTheme.CORAL };
                int bx = 0, by = 6 + offset;
                for (int i = 0; i < blocks.length; i++) {
                    g.setColor(UniversityPixelTheme.BLACK);
                    g.fillRect(bx + i * 16 + 2, by + 2, 14, 14);
                    g.setColor(blocks[i]);
                    g.fillRect(bx + i * 16, by, 14, 14);
                    g.setColor(new Color(255, 255, 255, 90));
                    g.fillRect(bx + i * 16 + 2, by + 2, 10, 3);
                }
                g.setFont(UniversityPixelTheme.font(13, Font.BOLD));
                g.setColor(MINT);
                g.drawString("TETRIS MONSTER", 90, by + 12);
                g.setFont(UniversityPixelTheme.font(big, Font.BOLD));
                FontMetrics m = g.getFontMetrics();
                int baseline = 22 + m.getAscent() + offset;
                String text = "CAMPUS QUEST";
                int depth = Math.max(3, big / 12);
                for (int d = depth; d > 0; d--) {
                    g.setColor(d == depth ? UniversityPixelTheme.BLACK : new Color(0x7A3B12));
                    g.drawString(text, d, baseline + d);
                }
                // 금색 글자 위를 지나가는 빛 줄기.
                g.setColor(GOLD);
                g.drawString(text, 0, baseline);
                int width = m.stringWidth(text);
                float shine = ((now % 3200) / 3200f) * 2.2f - 0.6f;
                Shape clip = g.getClip();
                g.clipRect(Math.round(width * shine) - 30, 0, 26, h);
                g.setColor(new Color(255, 255, 240));
                g.drawString(text, 0, baseline);
                g.setClip(clip);
                g.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
                g.setColor(UniversityPixelTheme.TEXT_SUB);
                g.drawString("블록을 지워 싸우는 캠퍼스 RPG  ·  대학교 → 졸업 → 취업", 2, baseline + 24);
            } finally { g.dispose(); }
        }
    }

    /** 다음에 도전할 스토리 전투. 몬스터가 숨 쉬듯 위아래로 움직인다. */
    private static final class NextBattleCard extends JComponent {
        String eyebrow = "NEXT BATTLE", title = "술", detail = "LV 1 · 일반 · 대학교 과정";
        String artKey = GameArt.keyForLevel(1);
        int cleared, total = 9;
        boolean boss;
        private ImageIcon cachedIcon;
        private String cachedKey;

        NextBattleCard() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("클릭하면 스토리 지도로 이동합니다.");
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int w = getWidth(), h = getHeight();
                long now = System.currentTimeMillis();
                Color accent = boss ? UniversityPixelTheme.CORAL : GOLD;
                g.setColor(new Color(0, 0, 0, 140));
                g.fillRect(6, 6, w - 6, h - 6);
                g.setColor(new Color(22, 38, 44, 232));
                g.fillRect(0, 0, w - 6, h - 6);
                g.setColor(accent);
                g.drawRect(0, 0, w - 7, h - 7);
                g.drawRect(1, 1, w - 9, h - 9);
                g.setFont(UniversityPixelTheme.font(11, Font.BOLD));
                g.fillRect(12, 12, g.getFontMetrics().stringWidth(eyebrow) + 12, 17);
                g.setColor(UniversityPixelTheme.BLACK);
                g.drawString(eyebrow, 18, 25);
                int art = Math.max(60, Math.min(w - 60, h - 150));
                int bob = (int) Math.round(Math.sin(now / 420.0) * 5);
                int ax = (w - 6 - art) / 2, ay = 34 + bob;
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                g.fillOval(ax + art / 8, ay + art / 8, art * 3 / 4, art * 3 / 4);
                g.setColor(new Color(0, 0, 0, 120));
                g.fillOval(ax + art / 4, 34 + art - 6, art / 2, 10);
                String key = artKey + ":" + art;
                if (!key.equals(cachedKey)) { cachedIcon = GameArt.icon(artKey, art, art); cachedKey = key; }
                ImageIcon icon = cachedIcon;
                if (icon != null) g.drawImage(icon.getImage(), ax, ay, null);
                int ty = 34 + art + 18;
                g.setFont(UniversityPixelTheme.font(12, Font.BOLD));
                g.setColor(accent);
                g.drawString(detail, 14, ty);
                g.setFont(UniversityPixelTheme.font(22, Font.BOLD));
                g.setColor(UniversityPixelTheme.TEXT);
                g.drawString(title, 14, ty + 25);
                int barY = h - 24, barW = w - 34;
                g.setColor(UniversityPixelTheme.BLACK);
                g.fillRect(14, barY, barW, 10);
                g.setColor(GOLD);
                g.fillRect(16, barY + 2, Math.round((barW - 4) * (cleared / (float) Math.max(1, total))), 6);
                g.setFont(UniversityPixelTheme.font(11, Font.BOLD));
                g.setColor(UniversityPixelTheme.TEXT_SUB);
                String progress = cleared + " / " + total + " CLEAR";
                g.drawString(progress, w - 20 - g.getFontMetrics().stringWidth(progress), barY - 6);
                if ((now / 500) % 2 == 0) {
                    g.setColor(UniversityPixelTheme.TEXT);
                    g.drawString("▶ 클릭해서 지도로", 14, barY - 6);
                }
            } finally { g.dispose(); }
        }
    }

    private static BufferedImage loadCampus() {
        try {
            return ImageIO.read(MainLobbyPanel.class.getResource("/ui/campus-rpg/university-bg.png"));
        } catch (IOException | IllegalArgumentException unavailable) {
            return null;
        }
    }

    public void setStoryAction(ActionListener listener) { storyButton.addActionListener(listener); }
    public void setOnlineBattleAction(ActionListener listener) { onlineButton.addActionListener(listener); }
    public void setLocalModeAction(ActionListener listener) { localButton.addActionListener(listener); }
    public void setCharacterAction(ActionListener listener) { characterButton.addActionListener(listener); }
    public void setTutorialAction(ActionListener listener) { tutorialButton.addActionListener(listener); }
    public void setServerAction(ActionListener listener) { serverButton.addActionListener(listener); }
    public void setSettingsAction(ActionListener listener) { settingsButton.addActionListener(listener); }
    public void setAccountStatus(String text) {
        boolean signedIn = text != null && text.startsWith(SIGNED_IN);
        String detail = signedIn ? text.substring(SIGNED_IN.length()).replaceFirst("^\\s*·\\s*", "") : text;
        UniversityPixelTheme.setChip(account, signedIn ? "● ONLINE  " + detail : detail,
                signedIn ? MINT : UniversityPixelTheme.PANEL_LIGHT);
        account.setToolTipText(text);
        onlineButton.setBadge(signedIn ? null : "로그인 필요", UniversityPixelTheme.PANEL_LIGHT);
        serverButton.setBadge(signedIn ? null : "로그인 필요", UniversityPixelTheme.PANEL_LIGHT);
        revalidate();
        repaint();
    }

    /** 스토리 진행도를 메뉴 배지와 다음 전투 카드에 보여 준다. */
    public void setStoryProgress(int cleared, int total) {
        storyButton.setText(cleared == 0 ? "스토리 시작" : cleared >= total ? "다시 플레이" : "이어하기");
        storyButton.setBadge(cleared + " / " + total, GOLD);
        nextCard.cleared = cleared; nextCard.total = total;
        repaint();
    }

    /** 다음 도전 전투. 모두 깼다면 마지막 보스를 다시 보여 준다. */
    public void setNextBattle(int level, String monsterName, String tierName, String chapterName, boolean boss, boolean allCleared) {
        nextCard.eyebrow = allCleared ? "ALL CLEAR · 다시 도전" : boss ? "BOSS BATTLE" : "NEXT BATTLE";
        nextCard.title = monsterName;
        nextCard.detail = "LV " + level + " · " + tierName + " · " + chapterName;
        nextCard.artKey = GameArt.keyForLevel(level);
        nextCard.boss = boss;
        repaint();
    }

    public void setCoins(int amount) {
        UniversityPixelTheme.setChip(coins, amount + " COINS", GOLD);
        revalidate();
        repaint();
    }
}
