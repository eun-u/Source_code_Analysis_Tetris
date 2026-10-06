package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class MainLobbyPanel extends JPanel {

    private GameButton storyButton;
    private GameButton onlineButton;
    private GameButton localButton;

    public MainLobbyPanel() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Main Lobby", SwingConstants.CENTER);

        JPanel menuPanel = new JPanel(new GridBagLayout());
        JPanel buttonPanel = new JPanel();

        storyButton = new GameButton("Story");
        onlineButton = new GameButton("Online Battle");
        localButton = new GameButton("Local Mode");

        buttonPanel.add(storyButton);
        buttonPanel.add(onlineButton);
        buttonPanel.add(localButton);

        menuPanel.add(buttonPanel);

        add(titleLabel, BorderLayout.NORTH);
        add(menuPanel, BorderLayout.CENTER);
    }

    public void setStoryAction(ActionListener listener) {
        storyButton.addActionListener(listener);
    }

    public void setOnlineBattleAction(ActionListener listener) {
        onlineButton.addActionListener(listener);
    }

    public void setLocalModeAction(ActionListener listener) {
        localButton.addActionListener(listener);
    }
}
