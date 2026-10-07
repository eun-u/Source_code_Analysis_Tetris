package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

public class LocalModePanel extends kr.ac.jbnu.se.tetris.ui.seongeun.components.ScenePanel {
    private final GameButton infiniteButton = new GameButton("시작하기");
    private final GameButton sprintButton = new GameButton("시작하기");
    private final GameButton tutorialButton = new GameButton("연습하기");
    private final GameButton backButton = new GameButton("로비로");

    public LocalModePanel() {
        infiniteButton.setName("infiniteModeStart");
        sprintButton.setName("sprintModeStart");
        tutorialButton.setName("tutorialModeStart");
        setLayout(new BorderLayout(0, UniversityPixelTheme.HEADER_GAP));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, UniversityPixelTheme.GUTTER, 18, UniversityPixelTheme.GUTTER));

        backButton.secondary();
        backButton.setPreferredSize(new Dimension(96, 38));
        add(UniversityPixelTheme.screenHeader("SOLO PLAY", "로컬 모드", backButton), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout()); center.setOpaque(false);
        JPanel choices = new JPanel(new GridLayout(1, 3, 14, 0)); choices.setOpaque(false);
        choices.add(mode("∞", "무한 모드", "제한 없이 블록을 쌓고\n최고 기록에 도전하세요.", "끝없이", infiniteButton,
                UniversityPixelTheme.MINT));
        choices.add(mode("40", "스프린트", "40줄을 지우는 데 걸린\n시간을 겨뤄 보세요.", "타임 어택", sprintButton,
                UniversityPixelTheme.GOLD));
        choices.add(mode("?", "튜토리얼", "이동·회전·HOLD·낙하를\n한 단계씩 따라 해 보세요.", "처음이라면", tutorialButton,
                UniversityPixelTheme.TEXT_SUB));
        choices.setPreferredSize(new Dimension(780, 320));
        center.add(choices); add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel("← → 이동   ·   ↑ ↓ 회전   ·   D 한 칸 낙하   ·   SPACE 즉시 낙하   ·   C HOLD   ·   P 일시정지",
                SwingConstants.CENTER);
        hint.setForeground(UniversityPixelTheme.TEXT_SUB);
        hint.setFont(UniversityPixelTheme.font(12, Font.PLAIN));
        add(hint, BorderLayout.SOUTH);
    }

    private JPanel mode(String symbol, String name, String description, String tag, GameButton action, Color accent) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UniversityPixelTheme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3),
                new EmptyBorder(18, 14, 20, 14)));
        JLabel badge = UniversityPixelTheme.chip(tag, accent);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setForeground(accent); icon.setFont(UniversityPixelTheme.font(46, Font.BOLD));
        JLabel heading = new JLabel(name, SwingConstants.CENTER);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setForeground(UniversityPixelTheme.TEXT);
        heading.setFont(UniversityPixelTheme.font(20, Font.BOLD));
        JLabel body = new JLabel("<html><center>" + description.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.setForeground(UniversityPixelTheme.TEXT_SUB);
        body.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
        action.setAccent(accent);
        action.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        action.setAlignmentX(Component.CENTER_ALIGNMENT);
        action.setMaximumSize(new Dimension(170, 44));
        action.setPreferredSize(new Dimension(170, 44));
        card.add(badge); card.add(Box.createVerticalStrut(10));
        card.add(icon); card.add(Box.createVerticalStrut(6)); card.add(heading);
        card.add(Box.createVerticalStrut(9)); card.add(body);
        card.add(Box.createVerticalGlue()); card.add(action);
        return card;
    }

    public void setInfiniteAction(ActionListener listener) { infiniteButton.addActionListener(listener); }
    public void setSprintAction(ActionListener listener) { sprintButton.addActionListener(listener); }
    public void setTutorialAction(ActionListener listener) { tutorialButton.addActionListener(listener); }
    public void setBackAction(ActionListener listener) { backButton.addActionListener(listener); }
}
