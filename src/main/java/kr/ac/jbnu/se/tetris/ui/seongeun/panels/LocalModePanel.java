package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class LocalModePanel extends JPanel {

    private GameButton infiniteButton;
    private GameButton sprintButton;
    private GameButton backButton;

    public LocalModePanel() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Local Mode", SwingConstants.CENTER);

        backButton = new GameButton("Back");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        infiniteButton = new GameButton("Infinite");
        sprintButton = new GameButton("Sprint");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(infiniteButton);
        buttonPanel.add(sprintButton);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(buttonPanel);

        add(centerPanel, BorderLayout.CENTER);
    }

    public void setInfiniteAction(ActionListener listener) {
        infiniteButton.addActionListener(listener);
    }

    public void setSprintAction(ActionListener listener) {
        sprintButton.addActionListener(listener);
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }
}
