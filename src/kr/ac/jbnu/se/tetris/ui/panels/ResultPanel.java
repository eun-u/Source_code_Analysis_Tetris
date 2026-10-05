package kr.ac.jbnu.se.tetris.ui.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

public class ResultPanel extends JPanel {

    private JLabel resultLabel;
    private JLabel playerNameLabel;
    private JLabel lineLabel;
    private JLabel comboLabel;
    private JLabel damageLabel;
    private JLabel rewardLabel;

    private GameButton returnButton;
    private GameButton lobbyButton;

    public ResultPanel() {
        setLayout(new BorderLayout());

        resultLabel = new JLabel("RESULT", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Dialog", Font.BOLD, 28));

        add(resultLabel, BorderLayout.NORTH);

        JPanel resultInfoPanel = new JPanel();
        resultInfoPanel.setLayout(new BoxLayout(resultInfoPanel, BoxLayout.Y_AXIS));

        playerNameLabel = new JLabel("Player");
        lineLabel = new JLabel("Line : 0");
        comboLabel = new JLabel("Max Combo : 0");
        damageLabel = new JLabel("Damage : 0");
        rewardLabel = new JLabel("Reward : 0");

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
        resultInfoPanel.add(Box.createVerticalGlue());

        add(resultInfoPanel, BorderLayout.CENTER);

        returnButton = new GameButton("돌아가기");
        lobbyButton = new GameButton("로비로");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(returnButton);
        buttonPanel.add(lobbyButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setResult(String result, String playerName, int line, int maxCombo, int damage, int reward) {
        resultLabel.setText(result);
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
}