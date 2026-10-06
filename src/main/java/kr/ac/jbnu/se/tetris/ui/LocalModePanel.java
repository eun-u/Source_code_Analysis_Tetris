package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 원본의 상단 제목·Back과 중앙 가로 모드 버튼을 유지한다. */
public final class LocalModePanel extends JPanel implements Screen {
    private final GameButton tutorialButton = new GameButton("튜토리얼");

    public LocalModePanel(Runnable infinite, Runnable sprint, Runnable back) {
        super(new BorderLayout());
        if (infinite == null || sprint == null || back == null) {
            throw new IllegalArgumentException("Actions are required");
        }
        JLabel titleLabel = new JLabel("Local Mode", SwingConstants.CENTER);
        GameButton backButton = new GameButton("Back");
        backButton.setName("localBack");
        backButton.addActionListener(event -> back.run());
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        GameButton infiniteButton = new GameButton("Infinite");
        GameButton sprintButton = new GameButton("Sprint");
        infiniteButton.setName("infinite");
        sprintButton.setName("sprint");
        infiniteButton.addActionListener(event -> infinite.run());
        sprintButton.addActionListener(event -> sprint.run());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(infiniteButton);
        buttonPanel.add(sprintButton);
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(buttonPanel);
        add(centerPanel, BorderLayout.CENTER);

        tutorialButton.setName("tutorial");
        tutorialButton.setFont(tutorialButton.getFont().deriveFont(11f));
        tutorialButton.setPreferredSize(new java.awt.Dimension(86, 26));
        tutorialButton.setEnabled(false);
        JPanel auxiliary = new JPanel();
        auxiliary.add(tutorialButton);
        add(auxiliary, BorderLayout.SOUTH);
    }

    public void setTutorialAction(Runnable action) {
        ScreenRouter.requireEdt();
        for (java.awt.event.ActionListener listener : tutorialButton.getActionListeners()) {
            tutorialButton.removeActionListener(listener);
        }
        tutorialButton.setEnabled(action != null);
        if (action != null) tutorialButton.addActionListener(event -> action.run());
    }
    @Override public String getId() { return "local-mode"; }
    @Override public JPanel getPanel() { return this; }
}
