package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** 캠퍼스의 하루에서 시작하는 게임 모드 선택 화면. */
public class MainLobbyPanel extends JPanel {
    private static final Color NIGHT = UniversityPixelTheme.BG;
    private static final Color CARD = UniversityPixelTheme.PANEL;
    private static final Color BORDER = UniversityPixelTheme.LINE;
    private static final BufferedImage CAMPUS = loadCampus();

    private final GameButton storyButton = new GameButton("Story");
    private final GameButton onlineButton = new GameButton("Online Battle");
    private final GameButton localButton = new GameButton("Local Mode");
    private final GameButton characterButton = new GameButton("캐릭터 / 상점");
    private final GameButton tutorialButton = new GameButton("조작법");
    private final GameButton serverButton = new GameButton("PvP 랭킹");
    private final GameButton settingsButton = new GameButton("설정");
    private final GameButton accountButton = new GameButton("온라인 계정");
    private final JLabel account = new JLabel("로컬 플레이", SwingConstants.RIGHT);

    public MainLobbyPanel() {
        setLayout(new BorderLayout());
        setBackground(NIGHT);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 2, 15, 2));
        JPanel title = column();
        title.add(label("TETRIS MONSTER  /  CAMPUS QUEST", 25, Font.BOLD, UniversityPixelTheme.TEXT));
        title.add(Box.createVerticalStrut(4));
        title.add(label("테트리스 한 판으로 대학부터 첫 출근까지", 13, Font.PLAIN,
                UniversityPixelTheme.TEXT_SUB));
        header.add(title, BorderLayout.CENTER);
        settingsButton.setAccent(UniversityPixelTheme.CORAL);
        settingsButton.setPreferredSize(new Dimension(104, 44));
        settingsButton.setToolTipText("효과음과 게임 설정 열기");
        header.add(settingsButton, BorderLayout.EAST);
        return header;
    }

    private JPanel content() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = 0; left.weightx = 0.59; left.weighty = 1;
        left.fill = GridBagConstraints.BOTH; left.insets = new Insets(0, 0, 0, 14);
        content.add(storyCard(), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = 0; right.weightx = 0.41; right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        content.add(modeCard(), right);
        return content;
    }

    private JPanel storyCard() {
        JPanel card = new CampusCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.BLACK, 4),
                BorderFactory.createEmptyBorder(22, 23, 22, 23)));
        JPanel intro = column();
        intro.add(label("01  /  STORY CAMPAIGN", 13, Font.BOLD, UniversityPixelTheme.GOLD));
        intro.add(Box.createVerticalStrut(11));
        intro.add(label("캠퍼스에서 첫 출근까지", 24, Font.BOLD, UniversityPixelTheme.TEXT));
        intro.add(Box.createVerticalStrut(8));
        intro.add(label("블록을 쌓아 싸우고, 아홉 번의 전투를 통과하세요.", 13,
                Font.PLAIN, UniversityPixelTheme.TEXT_SUB));
        intro.add(Box.createVerticalStrut(22));
        intro.add(chapterTrack());
        card.add(intro, BorderLayout.NORTH);

        JPanel action = new JPanel(new BorderLayout());
        action.setOpaque(false);
        storyButton.setAccent(UniversityPixelTheme.GOLD);
        storyButton.setPreferredSize(new Dimension(200, 51));
        action.add(storyButton, BorderLayout.WEST);
        JLabel hint = label("3개 장 · 9레벨", 12, Font.BOLD, UniversityPixelTheme.TEXT);
        hint.setHorizontalAlignment(SwingConstants.RIGHT);
        action.add(hint, BorderLayout.EAST);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private JPanel chapterTrack() {
        JPanel track = new JPanel(new GridLayout(1, 3, 8, 0));
        track.setOpaque(false);
        String[] number = {"01", "02", "03"};
        String[] name = {"대학교", "졸업", "취업"};
        String[] boss = {"교수", "캡스톤", "기업"};
        for (int index = 0; index < name.length; index++) {
            JPanel chapter = column();
            chapter.setOpaque(true);
            chapter.setBackground(UniversityPixelTheme.PANEL_LIGHT);
            chapter.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 2),
                    BorderFactory.createEmptyBorder(8, 9, 7, 8)));
            chapter.add(label(number[index] + "  " + name[index], 12, Font.BOLD,
                    UniversityPixelTheme.TEXT));
            chapter.add(Box.createVerticalStrut(3));
            chapter.add(label("보스  " + boss[index], 11, Font.PLAIN,
                    UniversityPixelTheme.TEXT_SUB));
            track.add(chapter);
        }
        return track;
    }

    private JPanel modeCard() {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.BLACK, 4),
                BorderFactory.createEmptyBorder(18, 16, 18, 16)));
        JPanel heading = column();
        heading.add(label("CHOOSE YOUR MODE", 12, Font.BOLD, UniversityPixelTheme.MINT));
        heading.add(Box.createVerticalStrut(5));
        heading.add(label("오늘은 어떤 승부?", 20, Font.BOLD, UniversityPixelTheme.TEXT));
        card.add(heading, BorderLayout.NORTH);

        JPanel modes = new JPanel(new GridLayout(3, 2, 9, 9));
        modes.setOpaque(false);
        onlineButton.setAccent(UniversityPixelTheme.MINT);
        localButton.setAccent(UniversityPixelTheme.GOLD);
        characterButton.setAccent(UniversityPixelTheme.GOLD);
        serverButton.setAccent(UniversityPixelTheme.MINT);
        tutorialButton.setAccent(UniversityPixelTheme.GOLD);
        accountButton.setAccent(UniversityPixelTheme.MINT);
        modes.add(modeTile(onlineButton, "계정으로 대전"));
        modes.add(modeTile(localButton, "혼자 연습하기"));
        modes.add(modeTile(characterButton, "내 캐릭터 관리"));
        modes.add(modeTile(serverButton, "공식 PvP 순위"));
        modes.add(modeTile(tutorialButton, "설정에서 연습하기"));
        modes.add(modeTile(accountButton, "로그인 / 로그아웃"));
        card.add(modes, BorderLayout.CENTER);
        return card;
    }

    private JPanel modeTile(GameButton button, String description) {
        JPanel tile = new JPanel(new BorderLayout(0, 5));
        tile.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 2),
                BorderFactory.createEmptyBorder(7, 6, 7, 6)));
        button.setPreferredSize(new Dimension(130, 45));
        tile.add(button, BorderLayout.CENTER);
        JLabel detail = label(description, 11, Font.PLAIN, UniversityPixelTheme.TEXT_SUB);
        detail.setHorizontalAlignment(SwingConstants.CENTER);
        tile.add(detail, BorderLayout.SOUTH);
        return tile;
    }

    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));
        footer.add(label("← → 이동   ↑ ↓ 회전   SPACE 착지   C 홀드", 11,
                Font.PLAIN, UniversityPixelTheme.TEXT_SUB), BorderLayout.WEST);
        account.setForeground(UniversityPixelTheme.TEXT_SUB);
        account.setFont(UniversityPixelTheme.font(11, Font.PLAIN));
        footer.add(account, BorderLayout.EAST);
        return footer;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JLabel label(String text, int size, int style, Color foreground) {
        JLabel label = new JLabel(text);
        label.setForeground(foreground);
        label.setFont(UniversityPixelTheme.font(size, style));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static BufferedImage loadCampus() {
        try {
            return ImageIO.read(MainLobbyPanel.class.getResource("/ui/university/campus-opening.png"));
        } catch (IOException | IllegalArgumentException unavailable) {
            return null;
        }
    }

    private static final class CampusCard extends JPanel {
        CampusCard() { setBackground(CARD); }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int width = getWidth(), height = getHeight();
                int artTop = (int) (height * 0.47);
                if (CAMPUS != null && width > 0 && height > artTop) {
                    int sourceHeight = CAMPUS.getHeight();
                    int sourceWidth = Math.min(CAMPUS.getWidth() - 300,
                            Math.max(1, width * sourceHeight / Math.max(1, height - artTop)));
                    int sourceX = Math.min(CAMPUS.getWidth() - sourceWidth,
                            Math.max(300, 920 - sourceWidth / 2));
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.drawImage(CAMPUS, 0, artTop, width, height,
                            sourceX, 0, sourceX + sourceWidth, sourceHeight, null);
                }
                g.setPaint(new GradientPaint(0, artTop - 14, CARD, 0, artTop + 100,
                        new Color(CARD.getRed(), CARD.getGreen(), CARD.getBlue(), 0)));
                g.fillRect(0, artTop - 14, width, 120);
                g.setColor(new Color(9, 6, 33, 170));
                g.fillRect(0, height - 91, width, 91);
                g.setColor(BORDER);
                for (int x = 32; x < width; x += 85) g.fillRect(x, artTop - 34, 3, 3);
            } finally { g.dispose(); }
        }
    }

    public void setStoryAction(ActionListener listener) { storyButton.addActionListener(listener); }
    public void setOnlineBattleAction(ActionListener listener) { onlineButton.addActionListener(listener); }
    public void setLocalModeAction(ActionListener listener) { localButton.addActionListener(listener); }
    public void setCharacterAction(ActionListener listener) { characterButton.addActionListener(listener); }
    public void setTutorialAction(ActionListener listener) { tutorialButton.addActionListener(listener); }
    public void setServerAction(ActionListener listener) { serverButton.addActionListener(listener); }
    public void setSettingsAction(ActionListener listener) { settingsButton.addActionListener(listener); }
    public void setAccountAction(ActionListener listener) { accountButton.addActionListener(listener); }
    public void setAccountStatus(String text) { account.setText(text); }
}
