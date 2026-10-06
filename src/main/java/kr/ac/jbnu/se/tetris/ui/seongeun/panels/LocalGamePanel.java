package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

import kr.ac.jbnu.se.tetris.ui.seongeun.Board;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.MiniPiecePreview;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PieceType;

public class LocalGamePanel extends JPanel {

    private JLabel titleLabel;
    private JLabel statusLabel;
    private Board playerBoard;
    private GameButton backButton;
    private final JLabel holdLabel = new JLabel("HOLD");
    private final JLabel nextLabel = new JLabel("NEXT");
    private final MiniPiecePreview holdPreview = new MiniPiecePreview("HOLD");
    private final MiniPiecePreview[] nextPreviews = {
        new MiniPiecePreview("1"), new MiniPiecePreview("2"), new MiniPiecePreview("3")
    };

    public LocalGamePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(13, 23, 40));

        titleLabel = new JLabel("Local Game", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        backButton = new GameButton("Back");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("0", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);

        playerBoard = new Board(statusLabel, true);
        playerBoard.setPreferredSize(new Dimension(280, 616));
        playerBoard.setMinimumSize(new Dimension(160, 352));
        playerBoard.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel boardArea = new JPanel(new GridBagLayout());
        boardArea.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1;
        c.fill = GridBagConstraints.BOTH; c.insets = new Insets(12, 24, 12, 12);
        boardArea.add(playerBoard, c);
        JPanel nextArea = new JPanel();
        nextArea.setOpaque(false);
        nextArea.setLayout(new BoxLayout(nextArea, BoxLayout.Y_AXIS));
        holdLabel.setForeground(Color.WHITE);
        nextLabel.setForeground(Color.WHITE);
        nextArea.add(holdLabel);
        nextArea.add(holdPreview);
        nextArea.add(Box.createVerticalStrut(12));
        nextArea.add(nextLabel);
        JPanel nextRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        nextRow.setOpaque(false);
        for (MiniPiecePreview preview : nextPreviews) nextRow.add(preview);
        nextArea.add(nextRow);
        nextArea.add(Box.createVerticalStrut(24));
        nextArea.add(lightLabel("← → 이동"));
        nextArea.add(lightLabel("↑ ↓ 회전"));
        nextArea.add(lightLabel("D 빠른 낙하"));
        nextArea.add(lightLabel("SPACE 즉시 낙하"));
        nextArea.add(lightLabel("C HOLD · P 일시정지"));
        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(20, 8, 20, 24);
        boardArea.add(nextArea, c);

        add(boardArea, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void startMode(String modeName) {
        titleLabel.setText("Local Mode - " + modeName);
        playerBoard.setOverlayText(null);

        playerBoard.start();

        SwingUtilities.invokeLater(() -> playerBoard.requestFocusInWindow());
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public Board getPlayerBoard() {
        return playerBoard;
    }

    public void setState(GameState state) {
        playerBoard.setState(state);
        holdPreview.setPiece(state.getHoldPiece(), state.getHoldItemId() != null);
        for (int index = 0; index < nextPreviews.length; index++)
            nextPreviews[index].setPiece(index < state.getNextPieces().size()
                    ? state.getNextPieces().get(index) : PieceType.EMPTY, false);
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }
    public void setCompleted(boolean completed) {
        playerBoard.setOverlayText(completed ? "SPRINT COMPLETE" : null);
        if (completed) statusLabel.setText("40 LINES CLEAR · 돌아가기로 종료");
    }
    private static JLabel lightLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(215, 232, 239));
        return label;
    }
}
