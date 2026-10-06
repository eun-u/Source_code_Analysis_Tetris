package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.resource.AssetManager;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 MainLobbyPanel의 제목·중앙 가로 메뉴 구성 */
public final class HomePanel extends JPanel implements Screen {
    private final JButton continueButton = new GameButton("계속하기");
    private final Runnable onEnter;

    public HomePanel(AssetManager assets, Runnable newGame, Runnable continueGame, Runnable onEnter) {
        this(assets, newGame, null, continueGame, onEnter);
    }

    public HomePanel(AssetManager assets, Runnable newGame, Runnable battle, Runnable continueGame, Runnable onEnter) {
        this(assets, newGame, battle, null, continueGame, onEnter);
    }

    public HomePanel(AssetManager assets, Runnable newGame, Runnable battle, Runnable onlineAction,
            Runnable continueGame, Runnable onEnter) {
        super(new BorderLayout());
        this.onEnter = onEnter;
        add(new JLabel("Main Lobby", JLabel.CENTER), BorderLayout.NORTH);

        JPanel menuPanel = new JPanel(new GridBagLayout());
        JPanel buttonPanel = new JPanel();
        JButton story = new GameButton("Story");
        story.setName("newBattle");
        story.setEnabled(battle != null);
        story.addActionListener(event -> { if (battle != null) battle.run(); });
        JButton online = new GameButton("Online Battle");
        online.setName("onlinePvP");
        online.setEnabled(onlineAction != null);
        online.addActionListener(event -> { if (onlineAction != null) onlineAction.run(); });
        JButton local = new GameButton("Local Mode");
        local.setName("newGame");
        local.addActionListener(event -> newGame.run());
        buttonPanel.add(story);
        buttonPanel.add(online);
        buttonPanel.add(local);
        menuPanel.add(buttonPanel);
        add(menuPanel, BorderLayout.CENTER);

        // 기존 진행 API와 테스트 조회용 버튼; 원본 MainLobbyPanel에는 표시하지 않음
        continueButton.setName("continueGame");
        continueButton.setVisible(false);
        continueButton.setEnabled(false);
        continueButton.addActionListener(event -> continueGame.run());
        add(continueButton, BorderLayout.SOUTH);
    }

    public void setCanContinue(boolean canContinue) { continueButton.setEnabled(canContinue); }
    public void setContinueText(String text) { continueButton.setText(text); }
    @Override public String getId() { return "home"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { onEnter.run(); }
}
