package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameArt;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameMenu;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/**
 * 게임 타이틀 화면. 화면 전체에 한국 캠퍼스 야경을 깔고, 왼쪽에 로고와 세로 메뉴,
 * 오른쪽 아래에 다음 스토리 전투 카드를 둔다. ↑↓·Enter로도 고를 수 있다.
 */
public class MainLobbyPanel extends JPanel {
    private static final BufferedImage CAMPUS = loadCampus();
    private static final BufferedImage LOBBY_LOGO = loadLobbyLogo();
    private static final BufferedImage COIN_IMAGE = loadCoinImage();
    private static final String SETTINGS_ICON_PATH = "/ui/campus-rpg/buttons/lobby_settings.png";
    private static final int SETTINGS_ICON_SIZE = 52;
    private static final String SIGNED_IN = "온라인 PvP 로그인 완료";
    private static final Color GOLD = UniversityPixelTheme.GOLD, MINT = UniversityPixelTheme.MINT;

    private final GameMenu menu = new GameMenu();
    private final GameMenu.Item storyButton = menu.add("스토리 시작", null, GOLD);
    private final GameMenu.Item onlineButton = menu.add("온라인 대전", null, MINT);
    private final GameMenu.Item localButton = menu.add("로컬 모드", null, MINT);
    private final GameMenu.Item tutorialButton = menu.add("튜토리얼", null, MINT);
    private final GameMenu.Item characterButton = menu.add("캐릭터 / 상점", null, GOLD);
    private final GameMenu.Item serverButton = menu.add("PvP 랭킹", null, MINT);
    private final JButton settingsButton = createSettingsIconButton();
    private final JLabel account = UniversityPixelTheme.chip("로컬 플레이", UniversityPixelTheme.PANEL_LIGHT);
    private final CoinBadge coins = new CoinBadge();
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
        tutorialButton.setImageResource("/ui/campus-rpg/buttons/lobby_tutorial.png");
        characterButton.setImageResource("/ui/campus-rpg/buttons/lobby_shop.png");
        serverButton.setImageResource("/ui/campus-rpg/buttons/lobby_ranking.png");
        // Match the user's local-mode PNG; if missing, GameMenu keeps its drawn fallback.
        localButton.setImageResource("/ui/campus-rpg/buttons/lobby_localmode.png");
        settingsButton.setToolTipText("소리 · 온라인 계정 · 튜토리얼 설정");
        account.setFont(UniversityPixelTheme.font(12, Font.BOLD));
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

    /** 우측 상단 설정 아이콘. 기존 버튼 클릭 이벤트는 그대로 연결된다. */
    private JButton createSettingsIconButton() {
        JButton button = new JButton();
        ImageIcon icon = loadScaledIcon(SETTINGS_ICON_PATH, SETTINGS_ICON_SIZE, SETTINGS_ICON_SIZE);
        if (icon != null) button.setIcon(icon);
        else button.setText("설정");
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(SETTINGS_ICON_SIZE, SETTINGS_ICON_SIZE));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** 프로젝트 리소스 PNG를 한 번만 읽어 버튼 아이콘으로 사용한다. */
    private static ImageIcon loadScaledIcon(String path, int width, int height) {
        URL url = MainLobbyPanel.class.getResource(path);
        if (url == null) {
            System.err.println("Missing resource: " + path);
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) return null;
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException error) {
            System.err.println("Cannot load icon: " + path + " (" + error.getMessage() + ")");
            return null;
        }
    }

    /** 코인 PNG가 왼쪽 둥근 끝을 덮는 캡슐형 배지. */
    private static final class CoinBadge extends JComponent {
        private static final int BADGE_HEIGHT = 52;
        private static final int ICON_SIZE = 50;
        private static final int BODY_HEIGHT = 40;
        private static final int TEXT_LEFT = 59;
        private static final int TEXT_RIGHT_PADDING = 11;
        private static final int MIN_WIDTH = 140;
        // 원본 PNG의 넓은 투명 여백을 무시하고 실제 코인 그림만 확대한다.
        private static final int[] COIN_CROP = findCoinCrop();

        private int value;

        CoinBadge() {
            setOpaque(false);
            setFont(UniversityPixelTheme.font(22, Font.BOLD));
            setToolTipText("보유 코인");
        }

        void setValue(int amount) {
            value = amount;
            revalidate();
            repaint();
        }

        /** 이미지 자체에 포함된 투명 여백을 제거할 잘라내기 좌표를 한 번만 계산. */
        private static int[] findCoinCrop() {
            if (COIN_IMAGE == null) return null;
            int left = COIN_IMAGE.getWidth();
            int top = COIN_IMAGE.getHeight();
            int right = -1;
            int bottom = -1;
            for (int y = 0; y < COIN_IMAGE.getHeight(); y++) {
                for (int x = 0; x < COIN_IMAGE.getWidth(); x++) {
                    int alpha = (COIN_IMAGE.getRGB(x, y) >>> 24) & 0xff;
                    if (alpha > 24) {
                        left = Math.min(left, x);
                        top = Math.min(top, y);
                        right = Math.max(right, x);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
            return right >= left && bottom >= top
                    ? new int[] {left, top, right + 1, bottom + 1} : null;
        }

        @Override
        public Dimension getPreferredSize() {
            String text = String.format(java.util.Locale.ROOT, "%,d", value);
            FontMetrics fm = getFontMetrics(getFont());
            int requiredWidth = TEXT_LEFT + fm.stringWidth(text) + TEXT_RIGHT_PADDING;
            return new Dimension(Math.max(MIN_WIDTH, requiredWidth), BADGE_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                // 배지의 둥근 왼쪽 끝은 코인 이미지 뒤로 숨겨진다.
                int iconSize = Math.min(ICON_SIZE, h);
                int iconY = (h - iconSize) / 2;
                int bodyX = iconSize / 2 - 1;
                int bodyH = Math.min(BODY_HEIGHT, h - 4);
                int bodyY = (h - bodyH) / 2;
                int bodyW = Math.max(1, w - bodyX - 3);

                // 부드러운 입체 그림자, 진한 외곽선, 진짜 알약 형태 배경.
                g.setColor(new Color(33, 37, 27, 105));
                g.fillRoundRect(bodyX + 1, bodyY + 3, bodyW, bodyH, bodyH, bodyH);
                g.setColor(new Color(54, 42, 31));
                g.fillRoundRect(bodyX, bodyY, bodyW, bodyH, bodyH, bodyH);
                g.setPaint(new GradientPaint(bodyX, bodyY + 2, new Color(107, 127, 78),
                        bodyX, bodyY + bodyH - 2, new Color(66, 82, 55)));
                g.fillRoundRect(bodyX + 2, bodyY + 2, Math.max(1, bodyW - 4), bodyH - 4,
                        bodyH - 4, bodyH - 4);
                g.setColor(new Color(207, 229, 172, 90));
                g.drawRoundRect(bodyX + 4, bodyY + 4, Math.max(1, bodyW - 8), bodyH - 8,
                        bodyH - 8, bodyH - 8);

                // 아이콘을 배경보다 나중에 그려 겹치는 부분을 깔끔하게 덮는다.
                if (COIN_IMAGE != null && COIN_CROP != null) {
                    g.drawImage(COIN_IMAGE, 0, iconY, iconSize, iconY + iconSize,
                            COIN_CROP[0], COIN_CROP[1], COIN_CROP[2], COIN_CROP[3], null);
                } else {
                    g.setColor(new Color(255, 207, 58));
                    g.fillOval(2, iconY + 2, iconSize - 4, iconSize - 4);
                    g.setColor(new Color(80, 44, 24));
                    g.drawOval(2, iconY + 2, iconSize - 5, iconSize - 5);
                }

                // 코인을 제외한 숫자 공간의 가로·세로 중앙 정렬.
                String text = String.format(java.util.Locale.ROOT, "%,d", value);
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                int textLeft = Math.min(TEXT_LEFT, w - 1);
                int textAreaWidth = Math.max(0, w - textLeft - TEXT_RIGHT_PADDING);
                int textX = textLeft + Math.max(0, (textAreaWidth - fm.stringWidth(text)) / 2);
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

                g.setColor(new Color(35, 41, 27, 185));
                g.drawString(text, textX + 1, textY + 2);
                g.setColor(Color.WHITE);
                g.drawString(text, textX, textY);
            } finally {
                g.dispose();
            }
        }
    }

    @Override public void doLayout() {
        int w = getWidth(), h = getHeight();
        int pad = UniversityPixelTheme.GUTTER;

        // 상단 3종 UI는 설정 아이콘(52px)을 기준으로 수직 중앙 정렬.
        Dimension settings = settingsButton.getPreferredSize();
        Dimension accountSize = account.getPreferredSize();
        Dimension coinSize = coins.getPreferredSize();
        int topY = 16;
        settingsButton.setBounds(w - pad - settings.width, topY, settings.width, settings.height);
        account.setBounds(settingsButton.getX() - 10 - accountSize.width,
                topY + (settings.height - accountSize.height) / 2,
                accountSize.width, accountSize.height);
        coins.setBounds(account.getX() - 8 - coinSize.width,
                topY + (settings.height - coinSize.height) / 2,
                coinSize.width, coinSize.height);

        // Keep the illustrated logo at its original size.
        int baseMenuWidth = Math.max(310, Math.min(430, w * 38 / 100));
        int logoHeight = Math.max(145, Math.min(248, Math.round(h * 0.40f)));
        int logoWidth = Math.min(baseMenuWidth, Math.round(logoHeight * (1448f / 1086f)));
        int logoX = pad + (baseMenuWidth - logoWidth) / 2;
        logo.setBounds(logoX, 14, logoWidth, logoHeight);

        // Scale six menu buttons, but retain their original gaps.
        int menuWidth = Math.round(baseMenuWidth * 0.90f);
        int menuX = pad + (baseMenuWidth - menuWidth) / 2;
        int menuY = logo.getY() + logoHeight + 8;
        int itemGap = 4; // Must match GameMenu GridLayout vertical gap.
        int availableMenuHeight = Math.max(200, h - menuY - pad);
        int baseItemHeight = Math.max(38, Math.min(76,
                (availableMenuHeight - itemGap * 5) / 6));
        int itemHeight = Math.round(baseItemHeight * 0.90f);
        int menuHeight = itemHeight * 6 + itemGap * 5;
        menu.setBounds(menuX, menuY, menuWidth, menuHeight);

        // Keep the existing next-battle card dimensions and placement.
        int cardWidth = Math.max(230, Math.min(330, w * 30 / 100));
        int cardHeight = Math.max(250, Math.min(380, h * 46 / 100));
        nextCard.setBounds(w - pad - cardWidth, h - 40 - cardHeight, cardWidth, cardHeight);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); // Clear the previous frame, including positions used before a resize.
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

    /** PNG logo replaces the old pixel blocks, TETRIS MONSTER, and CAMPUS QUEST lettering. */
    private static final class Logo extends JComponent {
        long shownAt;

        @Override protected void paintComponent(Graphics graphics) {
            if (LOBBY_LOGO == null || getWidth() <= 0 || getHeight() <= 0) return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                long age = shownAt == 0 ? 1000 : Math.max(0, System.currentTimeMillis() - shownAt);
                float progress = Math.min(1.0f, age / 350f);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress));

                // Fit inside the header without cropping or stretching the transparent PNG.
                double scale = Math.min(getWidth() / (double) LOBBY_LOGO.getWidth(),
                        getHeight() / (double) LOBBY_LOGO.getHeight());
                int dw = Math.max(1, (int) Math.round(LOBBY_LOGO.getWidth() * scale));
                int dh = Math.max(1, (int) Math.round(LOBBY_LOGO.getHeight() * scale));
                int x = (getWidth() - dw) / 2;
                int y = (getHeight() - dh) / 2 + Math.round((1 - progress) * -10);
                g.drawImage(LOBBY_LOGO, x, y, dw, dh, null);
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
            setOpaque(true);
            setBackground(new Color(0xF1EFE6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // No native Swing tooltip: it can extend beyond the right edge of the lobby.
            // The hand cursor and card click listener already indicate interactivity.
            setToolTipText(null);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); // The card has an opaque backing to avoid stale paint artifacts.
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.clipRect(0, 0, getWidth(), getHeight());
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

    private static BufferedImage loadLobbyLogo() {
        String path = "/ui/campus-rpg/tetris-monster-logo.png";
        try (InputStream stream = MainLobbyPanel.class.getResourceAsStream(path)) {
            if (stream == null) {
                System.err.println("Missing lobby logo image: " + path);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException error) {
            System.err.println("Cannot load lobby logo: " + error.getMessage());
            return null;
        }
    }

    private static BufferedImage loadCoinImage() {
        String path = "/ui/campus-rpg/icons/lobby_coin.png";
        try (InputStream stream = MainLobbyPanel.class.getResourceAsStream(path)) {
            if (stream == null) {
                System.err.println("Missing coin image: " + path);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException error) {
            System.err.println("Cannot load coin image: " + error.getMessage());
            return null;
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
        coins.setValue(amount);
        revalidate();
        repaint();
    }
}
