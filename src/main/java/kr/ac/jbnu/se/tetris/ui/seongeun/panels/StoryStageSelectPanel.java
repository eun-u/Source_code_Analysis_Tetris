package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.story.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.StoryProgressData;

/** 캠페인의 다섯 스테이지와 각 세 전투를 모두 선택하는 화면. */
public class StoryStageSelectPanel extends JPanel {
    private final JButton back = new PixelButton("로비로");
    private final StageCatalog catalog = StageCatalog.loadDefault();
    private final JButton[][] encounters = new JButton[catalog.getStages().size()][3];
    private final JLabel[] headings = new JLabel[catalog.getStages().size()];

    public StoryStageSelectPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(12, 22, 37));
        setBorder(new EmptyBorder(16, 22, 16, 22));
        JLabel title = new JLabel("STORY  /  MONSTER HUNT");
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(back, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (int index = 0; index < encounters.length; index++) {
            Stage stage = catalog.getStages().get(index);
            JPanel row = new JPanel(new BorderLayout(12, 8));
            row.setBackground(new Color(26, 42, 62));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(75, 109, 137), 2),
                    new EmptyBorder(12, 14, 12, 14)));
            headings[index] = new JLabel(String.format("%02d  %s", index + 1, stage.getName()));
            headings[index].setForeground(new Color(229, 242, 246));
            headings[index].setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
            row.add(headings[index], BorderLayout.NORTH);
            JPanel choices = new JPanel(new GridLayout(1, 3, 8, 0));
            choices.setOpaque(false);
            for (int tier = 0; tier < 3; tier++) {
                JButton button = new PixelButton(tierName(tier));
                button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
                button.setPreferredSize(new Dimension(150, 43));
                encounters[index][tier] = button;
                choices.add(button);
            }
            row.add(choices, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            list.add(row);
            list.add(Box.createVerticalStrut(10));
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(getBackground());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        JLabel foot = new JLabel("일반 → 엘리트 → 보스 순서로 해금됩니다.");
        foot.setForeground(new Color(166, 193, 208));
        add(foot, BorderLayout.SOUTH);
    }

    private static String tierName(int index) { return index == 0 ? "일반" : index == 1 ? "엘리트" : "보스"; }
    public void updateProgress(CampaignProgress progress, StageCatalog stages) {
        for (int i = 0; i < encounters.length; i++) {
            String stageId = stages.getStages().get(i).getId();
            boolean stageUnlocked = progress.isStageUnlocked(stageId);
            headings[i].setText(String.format("%02d  %s%s", i + 1, stages.getStages().get(i).getName(),
                    stageUnlocked ? "" : "  [잠김]"));
            for (int tier = 0; tier < 3; tier++) {
                MonsterSpec monster = stages.getStages().get(i).getEncounters().get(tier);
                boolean unlocked = progress.isEncounterUnlocked(stageId, monster.getTier());
                boolean cleared = progress.isEncounterCleared(monster.getId());
                encounters[i][tier].setEnabled(unlocked);
                encounters[i][tier].setText(tierName(tier) + (cleared ? "  ✓" : unlocked ? "" : "  🔒"));
                encounters[i][tier].setToolTipText(monster.getName() + " / HP " + monster.getHp());
            }
        }
    }
    /** 원본 미리보기 데이터 API 호환. */
    public void updateProgress(StoryProgressData progress) {
        for (int i = 0; i < Math.min(3, encounters.length); i++)
            for (int tier = 0; tier < 3; tier++) {
                boolean unlocked = progress.isUnlocked(i + 1, tier);
                encounters[i][tier].setEnabled(unlocked);
                encounters[i][tier].setText(unlocked ? tierName(tier) : "잠김");
            }
    }
    public void setBackAction(ActionListener listener) { back.addActionListener(listener); }
    public void setStageAction(int stageIndex, MonsterTier tier, ActionListener listener) {
        encounters[stageIndex][tier.ordinal()].addActionListener(listener);
    }
    public void setStage1NormalAction(ActionListener l) { setStageAction(0, MonsterTier.NORMAL, l); }
    public void setStage1EliteAction(ActionListener l) { setStageAction(0, MonsterTier.ELITE, l); }
    public void setStage1BossAction(ActionListener l) { setStageAction(0, MonsterTier.BOSS, l); }
    public void setStage2NormalAction(ActionListener l) { setStageAction(1, MonsterTier.NORMAL, l); }
    public void setStage2EliteAction(ActionListener l) { setStageAction(1, MonsterTier.ELITE, l); }
    public void setStage2BossAction(ActionListener l) { setStageAction(1, MonsterTier.BOSS, l); }
    public void setStage3NormalAction(ActionListener l) { setStageAction(2, MonsterTier.NORMAL, l); }
    public void setStage3EliteAction(ActionListener l) { setStageAction(2, MonsterTier.ELITE, l); }
    public void setStage3BossAction(ActionListener l) { setStageAction(2, MonsterTier.BOSS, l); }
}
