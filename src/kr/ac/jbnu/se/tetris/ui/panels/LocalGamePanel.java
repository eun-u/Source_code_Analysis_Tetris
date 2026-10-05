package kr.ac.jbnu.se.tetris.ui.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

import kr.ac.jbnu.se.tetris.Board;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

public class LocalGamePanel extends JPanel {

    private JLabel titleLabel;
    private JLabel statusLabel;
    private Board playerBoard;
    private GameButton backButton;

    public LocalGamePanel() {
        setLayout(new BorderLayout());

        titleLabel = new JLabel("Local Game", SwingConstants.CENTER);
        backButton = new GameButton("Back");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("0", SwingConstants.CENTER);

        playerBoard = new Board(statusLabel, true);
        playerBoard.setPreferredSize(new Dimension(240, 440));
        playerBoard.setMinimumSize(new Dimension(240, 440));
        playerBoard.setMaximumSize(new Dimension(240, 440));
        playerBoard.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel boardArea = new JPanel(new GridBagLayout());
        boardArea.add(playerBoard);

        add(boardArea, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void startMode(String modeName) {
        titleLabel.setText("Local Mode - " + modeName);

        playerBoard.start();

        SwingUtilities.invokeLater(() -> playerBoard.requestFocusInWindow());
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }
}