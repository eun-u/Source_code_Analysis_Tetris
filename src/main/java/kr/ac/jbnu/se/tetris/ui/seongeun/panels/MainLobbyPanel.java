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
    private final GameMenu.Item storyButton = menu.add("스토리 시작", null, GOLD);
    private final GameMenu.Item onlineButton = menu.add("온라인 대전", null, MINT);
    private final GameMenu.Item localButton = menu.add("로컬 모드", null, MINT);
    private final GameMenu.Item tutorialButton = menu.add("튜토리얼", null, MINT);
    private final GameMenu.Item characterButton = menu.add("캐릭터 / 상점", null, GOLD);
    private final GameMenu.Item serverButton = menu.add("PvP 랭킹", null, MINT);
    private final GameButton settingsButton = new GameButton("설정");
    private final JLabel account = UniversityPixelTheme.chip("로컬 플레이", UniversityPixelTheme.PANEL_LIGHT);
    private final JLabel coins = UniversityPixelTheme.chip("0 COINS", GOLD);
    private final Logo logo = new Logo();
    private final NextBattleCard nextCard = new NextBattleCard();
    private final Timer animation = new Timer(40, event -> repaint());

    public MainLobbyPanel() {
        setLayout(null);
        setBackground(UniversityPixelTheme.BG);
        storyButton.setName("lobbyStory");
        onlineButton.setName("lobbyOnline");
        localButton.setName("lobbyLocal");

        // Keep original GameMenu.Item buttons and their listeners; change only their rendering.
        storyButton.setImageResource("/ui/campus-rpg/buttons/lobby_story.png");
        onlineButton.setImageResource("/ui/campus-rpg/buttons/lobby_online.png");
        localButton.setImageResource("/ui/campus-rpg/buttons/lobby_localmode.png");
        tutorialButton.setImageResource("/ui/campus-rpg/buttons/lobby_tutorial.png");
        characterButton.setImageResource("/ui/campus-rpg/buttons/lobby_shop.png");
        serverButton.setImageResource("/ui/campus-rpg/buttons/lobby_ranking.png");
        settingsButton.secondary();
        settingsButton.setPreferredSize(new Dimension(84, 34));
        settingsButton.setToolTipText("소리 · 온라인 계정 · 튜토리얼 설정");
        account.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        coins.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        add(logo); add(menu); add(nextCard); add(account); add(coins); add(settingsButton);
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

    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        int pad = UniversityPixelTheme.GUTTER;

        // 1. 우측 상단 설정 버튼
        Dimension settings = settingsButton.getPreferredSize();
        settingsButton.setBounds(w - pad - settings.width, 16, settings.width, settings.height);

        // 2. 계정 정보 및 코인
        Dimension accountSize = account.getPreferredSize();
        Dimension coinSize = coins.getPreferredSize();

        account.setBounds(w - pad - settings.width - 10 - accountSize.width,
                22, accountSize.width, accountSize.height);

        coins.setBounds(account.getX() - 8 - coinSize.width,
                22, coinSize.width, coinSize.height);

        // 3. 메인 로고: 기존 크기 유지
        int baseMenuWidth = Math.max(310, Math.min(430, w * 38 / 100));

        int logoHeight = Math.max(145, Math.min(248, Math.round(h * 0.40f)));
        int logoWidth = Math.min(baseMenuWidth, Math.round(logoHeight * (1448f / 1086f)));
        int logoX = pad + (baseMenuWidth - logoWidth) / 2;

        logo.setBounds(logoX, 14, logoWidth, logoHeight);

        // 4. 메뉴 버튼 전체 너비: 기존 대비 83%
        int menuWidth = Math.round(baseMenuWidth * 0.90f);

        // 메뉴 버튼을 로고 아래 중앙에 배치
        int menuX = pad + (baseMenuWidth - menuWidth) / 2;
        int menuY = logo.getY() + logoHeight + 8;

        // 5. 메뉴 버튼 높이: 기존 대비 85%
        int itemGap = 4;
        int availableMenuHeight = Math.max(200, h - menuY - pad);

        int baseItemHeight = Math.max(38, Math.min(76,
                (availableMenuHeight - itemGap * 5) / 6));

        int itemHeight = Math.round(baseItemHeight * 0.90f);

        // 버튼 6개와 버튼 사이 간격 5개
        int menuHeight = itemHeight * 6 + itemGap * 5;

        menu.setBounds(menuX, menuY, menuWidth, menuHeight);

        // 6. NEXT BATTLE 카드: 기존 크기 유지
        int cardWidth = Math.max(230, Math.min(330, w * 30 / 100));
        int cardHeight = Math.max(250, Math.min(380, h * 46 / 100));

        nextCard.setBounds(w - pad - cardWidth,
                h - 40 - cardHeight, cardWidth, cardHeight);
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0xD9DBD2), 0, h, UniversityPixelTheme.BG));
            g.fillRect(0, 0, w, h);
            if (CAMPUS != null) {
                int artWidth = Math.max(w, CAMPUS.getWidth() * h / CAMPUS.getHeight());
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(CAMPUS, (w - artWidth) / 2, 0, artWidth, h, null);
            }
            // 배경은 공간감을 주되 메뉴와 제목 뒤에서는 연한 콘크리트색으로 물러난다.
            g.setPaint(new GradientPaint(0, 0, new Color(228, 229, 220, 205),
                    Math.round(w * 0.67f), 0, new Color(228, 229, 220, 18)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, h - 110, new Color(220, 222, 212, 0),
                    0, h, new Color(220, 222, 212, 184)));
            g.fillRect(0, h - 110, w, 110);
        } finally { g.dispose(); }
    }

    /** 블록이 떨어져 쌓이는 듯한 로고. 화면이 보일 때마다 짧게 등장 연출을 한다. */
    /** Draw only the transparent illustrated logo; never draw the legacy text logo. */
    private static final class Logo extends JComponent {
        private static final BufferedImage IMAGE = readLogoImage();
        long shownAt;

        private static BufferedImage readLogoImage() {
            String path = "/ui/campus-rpg/tetris-monster-logo.png";
            try (java.io.InputStream input = MainLobbyPanel.class.getResourceAsStream(path)) {
                if (input == null) {
                    System.err.println("Lobby logo resource missing: " + path);
                    return null;
                }
                return javax.imageio.ImageIO.read(input);
            } catch (java.io.IOException error) {
                System.err.println("Lobby logo failed to load: " + error.getMessage());
                return null;
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (IMAGE == null || getWidth() <= 0 || getHeight() <= 0) return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                double scale = Math.min(getWidth() / (double) IMAGE.getWidth(),
                        getHeight() / (double) IMAGE.getHeight());
                int imageW = Math.max(1, (int) Math.round(IMAGE.getWidth() * scale));
                int imageH = Math.max(1, (int) Math.round(IMAGE.getHeight() * scale));
                int x = (getWidth() - imageW) / 2;
                int y = (getHeight() - imageH) / 2;
                g.drawImage(IMAGE, x, y, imageW, imageH, null);
            } finally {
                g.dispose();
            }
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
                g.setColor(new Color(61, 72, 72, 63));
                g.fillRect(6, 6, w - 6, h - 6);
                g.setColor(new Color(241, 239, 230, 247));
                g.fillRect(0, 0, w - 6, h - 6);
                g.setColor(UniversityPixelTheme.LINE);
                g.drawRect(0, 0, w - 7, h - 7);
                g.setColor(accent);
                g.fillRect(1, 1, w - 9, 3);
                g.setFont(UniversityPixelTheme.font(11, Font.BOLD));
                g.fillRect(12, 12, g.getFontMetrics().stringWidth(eyebrow) + 12, 17);
                g.setColor(UniversityPixelTheme.BLACK);
                g.drawString(eyebrow, 18, 25);
                int art = Math.max(60, Math.min(w - 60, h - 150));
                int bob = (int) Math.round(Math.sin(now / 420.0) * 5);
                int ax = (w - 6 - art) / 2, ay = 34 + bob;
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                g.fillOval(ax + art / 8, ay + art / 8, art * 3 / 4, art * 3 / 4);
                g.setColor(new Color(62, 70, 67, 52));
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
                g.setColor(UniversityPixelTheme.LINE);
                g.fillRect(14, barY, barW, 10);
                g.setColor(GOLD);
                g.fillRect(16, barY + 2, Math.round((barW - 4) * (cleared / (float) Math.max(1, total))), 6);
                g.setFont(UniversityPixelTheme.font(11, Font.BOLD));
                g.setColor(UniversityPixelTheme.TEXT_SUB);
                String progress = cleared + " / " + total + " CLEAR";
                g.drawString(progress, w - 20 - g.getFontMetrics().stringWidth(progress), barY - 6);
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
        // The supplied PNG already says "스토리 시작". Show the dynamic state as a badge.
        String progress = cleared + " / " + total;
        storyButton.setBadge(cleared <= 0 ? progress
                : (cleared >= total ? "다시 플레이" : "이어하기") + " · " + progress, GOLD);
        storyButton.setToolTipText(storyButton.getText() + " · " + progress);
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
