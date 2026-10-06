package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.resource.AssetManager;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 MainLobbyPanel의 중앙 메뉴를 현재 앱 동작에 연결한 홈 화면 */
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
        super(new BorderLayout(0, 20));
        this.onEnter = onEnter;
        setBorder(BorderFactory.createEmptyBorder(24, 36, 20, 36));

        JLabel title = new JLabel("TETRIS · MAIN LOBBY", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(28f));
        add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel mascot = new JLabel(assets.getIcon("character.default", 72, 72));
        mascot.setToolTipText("기본 캐릭터 이미지");
        centered(content, mascot);
        content.add(Box.createVerticalStrut(20));

        JButton tutorial = new GameButton("튜토리얼");
        tutorial.setName("newGame");
        tutorial.addActionListener(event -> newGame.run());
        addAction(content, tutorial);

        JButton story = new GameButton("스토리 · 대학 도전");
        story.setName("newBattle");
        story.setEnabled(battle != null);
        story.addActionListener(event -> { if (battle != null) battle.run(); });
        addAction(content, story);

        JButton online = new GameButton(onlineAction == null ? "Online PvP · 준비 중" : "Online PvP · 로컬 서버");
        online.setName("onlinePvP");
        online.setEnabled(onlineAction != null);
        online.addActionListener(event -> { if (onlineAction != null) onlineAction.run(); });
        addAction(content, online);

        continueButton.setName("continueGame");
        continueButton.setEnabled(false);
        continueButton.addActionListener(event -> continueGame.run());
        addAction(content, continueButton);
        add(content, BorderLayout.CENTER);

        JLabel footer = new JLabel("튜토리얼 → 스토리 5개 Stage · 일반 → 엘리트 → 보스", JLabel.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private static void addAction(JPanel panel, JButton button) {
        button.setPreferredSize(new Dimension(250, 40));
        button.setMaximumSize(new Dimension(250, 40));
        centered(panel, button);
        panel.add(Box.createVerticalStrut(10));
    }

    private static void centered(JPanel panel, javax.swing.JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(component);
    }

    public void setCanContinue(boolean canContinue) { continueButton.setEnabled(canContinue); }
    public void setContinueText(String text) { continueButton.setText(text); }
    @Override public String getId() { return "home"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { onEnter.run(); }
}
