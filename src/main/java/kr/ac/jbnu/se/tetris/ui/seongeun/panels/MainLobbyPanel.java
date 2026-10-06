package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class MainLobbyPanel extends JPanel {

    private GameButton storyButton;
    private GameButton onlineButton;
    private GameButton localButton;
    private final GameButton characterButton = new GameButton("캐릭터 / 상점");
    private final GameButton tutorialButton = new GameButton("튜토리얼");
    private final GameButton serverButton = new GameButton("PvP 랭킹");
    private final JLabel account = new JLabel("로컬 플레이", SwingConstants.CENTER);

    public MainLobbyPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(13, 23, 40));

        JLabel titleLabel = new JLabel("TETRIS MONSTER  /  CAMPUS QUEST", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(32, 10, 20, 10));

        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setOpaque(false);
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 12, 12));
        buttonPanel.setOpaque(false);

        storyButton = new GameButton("Story");
        onlineButton = new GameButton("Online Battle");
        localButton = new GameButton("Local Mode");

        buttonPanel.add(storyButton);
        buttonPanel.add(onlineButton);
        buttonPanel.add(localButton);
        buttonPanel.add(characterButton);
        buttonPanel.add(tutorialButton);
        buttonPanel.add(serverButton);

        JPanel welcome = new JPanel(); welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        JPanel art = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); art.setOpaque(false);
        for (String key : new String[] {"university:2", "graduation:2", "employment:2"})
            art.add(new JLabel(kr.ac.jbnu.se.tetris.ui.seongeun.components.GameArt.icon(key, 130, 170)));
        welcome.add(art);
        JLabel journey = new JLabel("대학교  →  졸업  →  취업", SwingConstants.CENTER);
        journey.setAlignmentX(Component.CENTER_ALIGNMENT);
        journey.setForeground(new Color(255, 209, 102)); welcome.add(journey);
        welcome.add(Box.createVerticalStrut(22)); welcome.add(buttonPanel); menuPanel.add(welcome);

        add(titleLabel, BorderLayout.NORTH);
        add(menuPanel, BorderLayout.CENTER);
        account.setForeground(new Color(201, 193, 242));
        account.setBorder(BorderFactory.createEmptyBorder(12, 8, 20, 8));
        add(account, BorderLayout.SOUTH);
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
    public void setCharacterAction(ActionListener listener) { characterButton.addActionListener(listener); }
    public void setTutorialAction(ActionListener listener) { tutorialButton.addActionListener(listener); }
    public void setServerAction(ActionListener listener) { serverButton.addActionListener(listener); }
    public void setAccountStatus(String text) { account.setText(text); }
}
