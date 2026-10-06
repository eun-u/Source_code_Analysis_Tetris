package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

public class ResultPanel extends JPanel {
    private final JLabel resultLabel = new JLabel("RESULT", SwingConstants.CENTER);
    private final JLabel playerNameLabel = new JLabel("Player", SwingConstants.CENTER);
    private final JLabel lineLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel comboLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel damageLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel rewardLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel rankedStatus = new JLabel("", SwingConstants.CENTER);
    private final JLabel milestone = new JLabel("", SwingConstants.CENTER);
    private final GameButton returnButton = new GameButton("돌아가기");
    private final GameButton lobbyButton = new GameButton("로비로");
    private final GameButton nextButton = new GameButton("다음 전투");
    private final GameButton retryButton = new GameButton("다시 도전");

    public ResultPanel() {
        setLayout(new GridBagLayout());
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));
        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel heading = new JPanel(); heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("CAMPUS QUEST  /  전투 결과", SwingConstants.CENTER);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        eyebrow.setForeground(UniversityPixelTheme.TEXT_SUB);
        eyebrow.setFont(UniversityPixelTheme.font(13, Font.BOLD));
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultLabel.setFont(UniversityPixelTheme.font(33, Font.BOLD));
        resultLabel.setForeground(UniversityPixelTheme.TEXT);
        heading.add(eyebrow); heading.add(Box.createVerticalStrut(5)); heading.add(resultLabel);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(15));

        JPanel resultCard = new JPanel(new BorderLayout(0, 12));
        resultCard.setBackground(UniversityPixelTheme.PANEL);
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3),
                new EmptyBorder(16, 22, 16, 22)));
        resultCard.setPreferredSize(new Dimension(600, 390));
        resultCard.setMaximumSize(new Dimension(600, 390));
        resultCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerNameLabel.setForeground(UniversityPixelTheme.GOLD);
        playerNameLabel.setFont(UniversityPixelTheme.font(19, Font.BOLD));
        resultCard.add(playerNameLabel, BorderLayout.NORTH);
        JPanel stats = new JPanel(new GridLayout(2, 2, 10, 10)); stats.setOpaque(false);
        stats.add(stat("지운 줄", lineLabel)); stats.add(stat("최대 콤보", comboLabel));
        stats.add(stat("입힌 피해", damageLabel)); stats.add(stat("획득 코인", rewardLabel));
        resultCard.add(stats, BorderLayout.CENTER);
        JPanel notes = new JPanel(); notes.setOpaque(false);
        notes.setLayout(new BoxLayout(notes, BoxLayout.Y_AXIS));
        for (JLabel label : new JLabel[] { milestone, rankedStatus }) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setForeground(UniversityPixelTheme.GOLD);
            label.setFont(UniversityPixelTheme.font(14, Font.BOLD));
            notes.add(label);
        }
        resultCard.add(notes, BorderLayout.SOUTH);
        content.add(resultCard); content.add(Box.createVerticalStrut(14));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(returnButton);
        nextButton.setVisible(false); retryButton.setVisible(false);
        buttonPanel.add(nextButton); buttonPanel.add(retryButton); buttonPanel.add(lobbyButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(buttonPanel);
        add(content);
    }

    private JPanel stat(String name, JLabel value) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        cell.setBorder(new EmptyBorder(7, 8, 7, 8));
        JLabel caption = new JLabel(name, SwingConstants.CENTER);
        caption.setForeground(UniversityPixelTheme.TEXT_SUB);
        caption.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        value.setForeground(UniversityPixelTheme.TEXT);
        value.setFont(UniversityPixelTheme.font(21, Font.BOLD));
        cell.add(caption, BorderLayout.NORTH); cell.add(value, BorderLayout.CENTER);
        return cell;
    }

    public void setResult(String result, String playerName, int line, int maxCombo, int damage, int reward) {
        setResultDetails(result, playerName, String.valueOf(line), String.valueOf(maxCombo),
                String.valueOf(damage), String.valueOf(reward));
    }

    /** 미집계 항목도 원본 정보 자리에서 그대로 표시할 수 있게 한다. */
    public void setResultDetails(String result, String playerName, String line,
                                 String maxCombo, String damage, String reward) {
        resultLabel.setText(result);
        resultLabel.setForeground("VICTORY".equals(result) || "COMPLETE".equals(result)
                ? UniversityPixelTheme.MINT : "DEFEAT".equals(result)
                ? UniversityPixelTheme.CORAL : UniversityPixelTheme.TEXT);
        playerNameLabel.setText(playerName);
        lineLabel.setText(line); comboLabel.setText(maxCombo);
        damageLabel.setText(damage); rewardLabel.setText(reward);
    }

    public void setReturnButtonText(String text) { returnButton.setText(text); }
    public void setReturnAction(ActionListener listener) { returnButton.addActionListener(listener); }
    public void setLobbyAction(ActionListener listener) { lobbyButton.addActionListener(listener); }
    public void setRankedStatus(String text) { rankedStatus.setText(text); }
    public void setStoryActions(boolean story, boolean hasNext, String earnedBadge) {
        nextButton.setVisible(story && hasNext); retryButton.setVisible(story);
        milestone.setText(earnedBadge == null ? "" : "획득  /  " + earnedBadge);
    }
    public void setNextAction(ActionListener action) { nextButton.addActionListener(action); }
    public void setRetryAction(ActionListener action) { retryButton.addActionListener(action); }
}
