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

/** 실행 가능한 기능만 제공하는 홈 화면 */
public final class HomePanel extends JPanel implements Screen {
    private final JButton continueButton = new JButton("계속하기");
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
        setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("TETRIS");
        title.setFont(title.getFont().deriveFont(32f));
        addCentered(content, title);
        content.add(Box.createVerticalStrut(20));
        JLabel mascot = new JLabel(assets.getIcon("character.default", 96, 96));
        mascot.setToolTipText("기본 캐릭터 Placeholder");
        addCentered(content, mascot);
        content.add(Box.createVerticalStrut(24));
        JButton start = new JButton("튜토리얼");
        start.setName("newGame");
        start.addActionListener(event -> newGame.run());
        continueButton.setName("continueGame");
        continueButton.addActionListener(event -> continueGame.run());
        continueButton.setEnabled(false);
        JButton battleButton = new JButton("스토리 · 대학 도전");
        battleButton.setName("newBattle");
        battleButton.setEnabled(battle != null);
        battleButton.addActionListener(event -> { if (battle != null) battle.run(); });
        for (JButton button : new JButton[] {start, battleButton, continueButton}) {
            button.setMaximumSize(new Dimension(240, 40));
            addCentered(content, button);
            content.add(Box.createVerticalStrut(10));
        }
        content.add(Box.createVerticalStrut(24));
        JButton online = new JButton(onlineAction == null ? "Online PvP · 준비 중" : "Online PvP · 로컬 서버");
        online.setName("onlinePvP"); online.setEnabled(onlineAction != null);
        online.addActionListener(event -> { if (onlineAction != null) onlineAction.run(); });
        online.setMaximumSize(new Dimension(240, 40)); addCentered(content, online);
        addCentered(content, new JLabel("<html><b>조작 안내</b><br><br>← / → : 이동<br>↑ : 왼쪽 회전 / ↓ : 오른쪽 회전"
                + "<br>D : 한 칸 낙하 / Space : 즉시 낙하<br>C : HOLD (고정당 1회)<br>P : 로컬 일시정지 / 계속<br>Esc : 홈 (온라인은 대전 나가기)</html>"));
        add(content, BorderLayout.NORTH);
        add(new JLabel("튜토리얼 / 대학 스토리 · 일반 → 엘리트 → 보스", JLabel.CENTER), BorderLayout.SOUTH);
    }

    private static void addCentered(JPanel panel, javax.swing.JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(component);
    }
    public void setCanContinue(boolean canContinue) { continueButton.setEnabled(canContinue); }
    public void setContinueText(String text) { continueButton.setText(text); }
    @Override public String getId() { return "home"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { onEnter.run(); }
}
