package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** 게임 중에는 숨기는 오디오·조작 학습·LAN 설정 화면. */
public final class SettingsPanel extends JPanel {
    private final PixelButton back = new PixelButton("로비로");
    private final PixelButton tutorial = new PixelButton("5단계 튜토리얼 직접 해보기");
    private final PixelButton lanConnect = new PixelButton("LAN 서버 연결...");
    private final PixelButton lanHost = new PixelButton("LAN 서버 시작");
    private final JCheckBox skipTutorial = new JCheckBox("다시 보지 않기");
    private final JCheckBox bgmMute = new JCheckBox("BGM 끄기");
    private final JCheckBox sfxMute = new JCheckBox("효과음 끄기");
    private final JSlider bgmVolume = slider();
    private final JSlider sfxVolume = slider();
    private boolean loading;

    public SettingsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 24, 20, 24));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel title = text("설정  /  CAMPUS GUIDE", 24, UniversityPixelTheme.GOLD);
        header.add(title, BorderLayout.WEST); header.add(back, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0)); columns.setOpaque(false);
        JPanel audio = section("소리", "배경음과 전투 효과음을 따로 조절합니다.");
        row(audio, "BGM", bgmMute, bgmVolume);
        audio.add(Box.createVerticalStrut(22));
        row(audio, "효과음", sfxMute, sfxVolume);
        audio.add(Box.createVerticalGlue());
        columns.add(audio);

        JPanel guide = section("튜토리얼", "실제 블록을 움직이며 다섯 동작을 익힙니다.");
        for (String step : new String[] {
                "① ← →  이동", "② ↑ ↓  회전", "③ D  한 칸 내리기",
                "④ C  HOLD 보관", "⑤ SPACE  즉시 낙하" }) {
            JLabel line = text(step, 16, UniversityPixelTheme.TEXT);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            guide.add(line); guide.add(Box.createVerticalStrut(8));
        }
        guide.add(Box.createVerticalStrut(6));
        tutorial.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorial.setMaximumSize(new Dimension(440, 48)); guide.add(tutorial);
        guide.add(Box.createVerticalStrut(10));
        skipTutorial.setOpaque(false); skipTutorial.setForeground(UniversityPixelTheme.TEXT);
        skipTutorial.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        skipTutorial.setAlignmentX(Component.LEFT_ALIGNMENT); guide.add(skipTutorial);
        guide.add(Box.createVerticalGlue());
        columns.add(guide); add(columns, BorderLayout.CENTER);

        JPanel lan = new JPanel(new BorderLayout(16, 0));
        lan.setBackground(UniversityPixelTheme.PANEL);
        lan.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(8, 12, 8, 12)));
        lan.add(text("같은 네트워크의 LAN 대전", 14, UniversityPixelTheme.TEXT_SUB), BorderLayout.WEST);
        JPanel lanButtons = new JPanel(new GridLayout(1, 2, 8, 0)); lanButtons.setOpaque(false);
        lanButtons.add(lanConnect); lanButtons.add(lanHost); lan.add(lanButtons, BorderLayout.EAST);
        add(lan, BorderLayout.SOUTH);
    }

    private static JSlider slider() {
        JSlider slider = new JSlider(0, 100, 65);
        slider.setOpaque(false);
        slider.setForeground(UniversityPixelTheme.GOLD);
        slider.setMaximumSize(new Dimension(420, 45));
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setMajorTickSpacing(25); slider.setPaintTicks(true);
        return slider;
    }
    private static JLabel text(String value, int size, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(UniversityPixelTheme.font(size, Font.BOLD));
        label.setForeground(color);
        return label;
    }
    private static JPanel section(String heading, String explanation) {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UniversityPixelTheme.PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(16, 18, 16, 18)));
        JLabel title = text(heading, 21, UniversityPixelTheme.GOLD);
        title.setAlignmentX(Component.LEFT_ALIGNMENT); panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        JLabel subtitle = text(explanation, 13, UniversityPixelTheme.TEXT_SUB);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT); panel.add(subtitle);
        panel.add(Box.createVerticalStrut(22));
        return panel;
    }
    private static void row(JPanel parent, String label, JCheckBox mute, JSlider volume) {
        JLabel heading = text(label, 17, UniversityPixelTheme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT); parent.add(heading);
        parent.add(Box.createVerticalStrut(6));
        volume.setAlignmentX(Component.LEFT_ALIGNMENT); parent.add(volume);
        mute.setOpaque(false); mute.setForeground(UniversityPixelTheme.TEXT_SUB);
        mute.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        mute.setAlignmentX(Component.LEFT_ALIGNMENT); parent.add(mute);
    }
    public void update(boolean bgmOff, float bgmLevel, boolean sfxOff,
            float sfxLevel, boolean tutorialSkipped) {
        loading = true;
        try {
            bgmMute.setSelected(bgmOff); bgmVolume.setValue(Math.round(bgmLevel * 100));
            sfxMute.setSelected(sfxOff); sfxVolume.setValue(Math.round(sfxLevel * 100));
            skipTutorial.setSelected(tutorialSkipped);
        } finally { loading = false; }
    }
    public void setBackAction(ActionListener listener) { back.addActionListener(listener); }
    public void setTutorialAction(ActionListener listener) { tutorial.addActionListener(listener); }
    public void setLanConnectAction(ActionListener listener) { lanConnect.addActionListener(listener); }
    public void setLanHostAction(ActionListener listener) { lanHost.addActionListener(listener); }
    public void setBgmMuteAction(Consumer<Boolean> action) {
        bgmMute.addActionListener(event -> { if (!loading) action.accept(bgmMute.isSelected()); });
    }
    public void setSfxMuteAction(Consumer<Boolean> action) {
        sfxMute.addActionListener(event -> { if (!loading) action.accept(sfxMute.isSelected()); });
    }
    public void setBgmVolumeAction(Consumer<Float> action) {
        bgmVolume.addChangeListener(event -> { if (!loading) action.accept(bgmVolume.getValue() / 100f); });
    }
    public void setSfxVolumeAction(Consumer<Float> action) {
        sfxVolume.addChangeListener(event -> { if (!loading) action.accept(sfxVolume.getValue() / 100f); });
    }
    public void setSkipTutorialAction(Consumer<Boolean> action) {
        skipTutorial.addActionListener(event -> { if (!loading) action.accept(skipTutorial.isSelected()); });
    }
}
