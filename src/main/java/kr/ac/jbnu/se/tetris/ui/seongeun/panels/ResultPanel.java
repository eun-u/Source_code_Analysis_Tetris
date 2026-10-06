package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class ResultPanel extends JPanel {

    private JLabel resultLabel;
    private JLabel playerNameLabel;
    private JLabel lineLabel;
    private JLabel comboLabel;
    private JLabel damageLabel;
    private JLabel rewardLabel;

    private GameButton returnButton;
    private GameButton lobbyButton;
    private final JLabel rankedStatus = new JLabel("", SwingConstants.CENTER);
    private final JLabel milestone = new JLabel("", SwingConstants.CENTER);
    private final GameButton nextButton = new GameButton("다음 전투");
    private final GameButton retryButton = new GameButton("다시 도전");

    public ResultPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(13, 23, 40));

        resultLabel = new JLabel("RESULT", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Dialog", Font.BOLD, 28));
        resultLabel.setForeground(Color.WHITE);

        add(resultLabel, BorderLayout.NORTH);

        JPanel resultInfoPanel = new JPanel();
        resultInfoPanel.setOpaque(false);
        resultInfoPanel.setLayout(new BoxLayout(resultInfoPanel, BoxLayout.Y_AXIS));

        playerNameLabel = new JLabel("Player");
        lineLabel = new JLabel("Line : 0");
        comboLabel = new JLabel("Max Combo : 0");
        damageLabel = new JLabel("Damage : 0");
        rewardLabel = new JLabel("Reward : 0");
        for (JLabel detail : new JLabel[] { playerNameLabel, lineLabel, comboLabel,
                damageLabel, rewardLabel }) detail.setForeground(new Color(220, 235, 241));

        playerNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        lineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        comboLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        damageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rewardLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultInfoPanel.add(Box.createVerticalGlue());
        resultInfoPanel.add(playerNameLabel);
        resultInfoPanel.add(Box.createVerticalStrut(20));
        resultInfoPanel.add(lineLabel);
        resultInfoPanel.add(Box.createVerticalStrut(10));
        resultInfoPanel.add(comboLabel);
        resultInfoPanel.add(Box.createVerticalStrut(10));
        resultInfoPanel.add(damageLabel);
        resultInfoPanel.add(Box.createVerticalStrut(10));
        resultInfoPanel.add(rewardLabel);
        resultInfoPanel.add(Box.createVerticalStrut(18));
        rankedStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        rankedStatus.setForeground(new Color(255, 209, 102));
        resultInfoPanel.add(rankedStatus);
        milestone.setAlignmentX(Component.CENTER_ALIGNMENT);
        milestone.setFont(new Font("Dialog", Font.BOLD, 24));
        milestone.setForeground(new Color(255, 209, 102));
        resultInfoPanel.add(Box.createVerticalStrut(16)); resultInfoPanel.add(milestone);
        resultInfoPanel.add(Box.createVerticalGlue());

        add(resultInfoPanel, BorderLayout.CENTER);

        returnButton = new GameButton("돌아가기");
        lobbyButton = new GameButton("로비로");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(returnButton);
        nextButton.setVisible(false); retryButton.setVisible(false);
        buttonPanel.add(nextButton); buttonPanel.add(retryButton);
        buttonPanel.add(lobbyButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setResult(String result, String playerName, int line, int maxCombo, int damage, int reward) {
        resultLabel.setText(result);
        resultLabel.setForeground("VICTORY".equals(result) || "COMPLETE".equals(result)
                ? new Color(126, 226, 170) : "DEFEAT".equals(result)
                ? new Color(255, 145, 133) : Color.WHITE);
        playerNameLabel.setText(playerName);
        lineLabel.setText("Line : " + line);
        comboLabel.setText("Max Combo : " + maxCombo);
        damageLabel.setText("Damage : " + damage);
        rewardLabel.setText("Reward : " + reward);
    }

    /** 미집계 항목도 원본 정보 자리에서 그대로 표시할 수 있게 한다. */
    public void setResultDetails(String result, String playerName, String line,
                                 String maxCombo, String damage, String reward) {
        resultLabel.setText(result);
        resultLabel.setForeground("VICTORY".equals(result) || "COMPLETE".equals(result)
                ? new Color(126, 226, 170) : "DEFEAT".equals(result)
                ? new Color(255, 145, 133) : Color.WHITE);
        playerNameLabel.setText(playerName);
        lineLabel.setText("Line : " + line);
        comboLabel.setText("Max Combo : " + maxCombo);
        damageLabel.setText("Damage : " + damage);
        rewardLabel.setText("Reward : " + reward);
    }

    public void setReturnButtonText(String text) {
        returnButton.setText(text);
    }

    public void setReturnAction(ActionListener listener) {
        returnButton.addActionListener(listener);
    }

    public void setLobbyAction(ActionListener listener) {
        lobbyButton.addActionListener(listener);
    }
    public void setRankedStatus(String text) { rankedStatus.setText(text); }
    public void setStoryActions(boolean story, boolean hasNext, String earnedBadge) {
        nextButton.setVisible(story && hasNext); retryButton.setVisible(story);
        milestone.setText(earnedBadge == null ? "" : earnedBadge);
    }
    public void setNextAction(ActionListener action) { nextButton.addActionListener(action); }
    public void setRetryAction(ActionListener action) { retryButton.addActionListener(action); }
}
