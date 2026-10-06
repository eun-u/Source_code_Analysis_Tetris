package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

public class LocalModePanel extends JPanel {
    private final GameButton infiniteButton = new GameButton("시작하기");
    private final GameButton sprintButton = new GameButton("시작하기");
    private final GameButton backButton = new GameButton("로비로");

    public LocalModePanel() {
        infiniteButton.setName("infiniteModeStart");
        sprintButton.setName("sprintModeStart");
        setLayout(new BorderLayout(0, 16));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel title = new JLabel("CAMPUS QUEST  /  로컬 모드");
        title.setForeground(UniversityPixelTheme.GOLD);
        title.setFont(UniversityPixelTheme.font(23, Font.BOLD));
        top.add(title, BorderLayout.WEST); top.add(backButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout()); center.setOpaque(false);
        JPanel choices = new JPanel(new GridLayout(1, 2, 16, 0)); choices.setOpaque(false);
        choices.add(mode("∞", "무한 모드", "제한 없이 블록을 쌓고\n최고 기록에 도전하세요.", infiniteButton,
                UniversityPixelTheme.MINT));
        choices.add(mode("40", "스프린트", "40줄을 가장 빠르게\n지워 보세요.", sprintButton,
                UniversityPixelTheme.GOLD));
        choices.setPreferredSize(new Dimension(620, 300));
        center.add(choices); add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel("키보드  ← → 이동   ·   ↑ ↓ 회전   ·   SPACE 즉시 내리기", SwingConstants.CENTER);
        hint.setForeground(UniversityPixelTheme.TEXT_SUB);
        hint.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
        add(hint, BorderLayout.SOUTH);
    }

    private JPanel mode(String symbol, String name, String description, GameButton action, Color accent) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UniversityPixelTheme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3),
                new EmptyBorder(18, 12, 18, 12)));
        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setForeground(accent); icon.setFont(UniversityPixelTheme.font(44, Font.BOLD));
        JLabel heading = new JLabel(name, SwingConstants.CENTER);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setForeground(UniversityPixelTheme.TEXT);
        heading.setFont(UniversityPixelTheme.font(20, Font.BOLD));
        JLabel body = new JLabel("<html><center>" + description.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.setForeground(UniversityPixelTheme.TEXT_SUB);
        body.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
        action.setAlignmentX(Component.CENTER_ALIGNMENT);
        action.setMaximumSize(new Dimension(160, 38));
        card.add(icon); card.add(Box.createVerticalStrut(8)); card.add(heading);
        card.add(Box.createVerticalStrut(9)); card.add(body);
        card.add(Box.createVerticalGlue()); card.add(action);
        return card;
    }

    public void setInfiniteAction(ActionListener listener) { infiniteButton.addActionListener(listener); }
    public void setSprintAction(ActionListener listener) { sprintButton.addActionListener(listener); }
    public void setBackAction(ActionListener listener) { backButton.addActionListener(listener); }
}
