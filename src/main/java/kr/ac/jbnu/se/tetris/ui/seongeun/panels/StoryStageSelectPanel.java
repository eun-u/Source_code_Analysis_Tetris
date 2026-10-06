package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ai.DifficultyProfile;
import kr.ac.jbnu.se.tetris.ai.DifficultyProfileCatalog;
import kr.ac.jbnu.se.tetris.story.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.StoryProgressData;

/** 대학교 → 졸업 → 취업, 세 과정의 아홉 전투를 ID로 선택한다. */
public class StoryStageSelectPanel extends JPanel {
    private final JButton back = new PixelButton("로비로");
    private final StageCatalog catalog = StageCatalog.loadDefault();
    private final Map<String, JButton> encounters = new LinkedHashMap<>();
    private final Map<String, JLabel> states = new LinkedHashMap<>();
    private final JLabel completion = new JLabel("0 / 9 CLEAR");

    public StoryStageSelectPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(16, 22, 16, 22));
        JPanel header = new JPanel(new BorderLayout(12, 0)); header.setOpaque(false);
        JLabel title = new JLabel("CAMPUS QUEST  /  스토리");
        title.setFont(UniversityPixelTheme.font(24, Font.BOLD));
        title.setForeground(UniversityPixelTheme.GOLD); header.add(title, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false); right.add(completion); right.add(back);
        header.add(right, BorderLayout.EAST); add(header, BorderLayout.NORTH);
        JPanel chapters = new JPanel(new GridLayout(3, 1, 0, 10));
        chapters.setOpaque(false);
        int level = 0;
        for (Stage stage : catalog.getStages()) {
            JPanel chapter = new JPanel(new BorderLayout(0, 5));
            chapter.setOpaque(false);
            JLabel intro = new JLabel(stage.getName() + "   /   " + introduction(stage.getId()));
            intro.setForeground(UniversityPixelTheme.GOLD);
            intro.setFont(UniversityPixelTheme.font(14, Font.BOLD));
            chapter.add(intro, BorderLayout.NORTH);
            JPanel cards = new JPanel(new GridLayout(1, 3, 8, 0)); cards.setOpaque(false);
            for (MonsterSpec monster : stage.getEncounters()) {
                level++;
                DifficultyProfile difficulty = DifficultyProfileCatalog.forEncounter(monster);
                JPanel card = new JPanel(new BorderLayout(8, 2));
                card.setBackground(UniversityPixelTheme.PANEL);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(monster.getTier() == MonsterTier.BOSS
                                ? UniversityPixelTheme.GOLD : UniversityPixelTheme.LINE, 2),
                        new EmptyBorder(7, 9, 7, 9)));
                JLabel art = new JLabel(GameArt.icon(GameArt.keyForLevel(level), 102, 106));
                card.add(art, BorderLayout.WEST);
                JPanel details = new JPanel(); details.setOpaque(false);
                details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                line(details, "LV " + level + "  /  " + tierName(monster.getTier()), 12, UniversityPixelTheme.GOLD);
                line(details, monster.getName(), 18, UniversityPixelTheme.TEXT);
                details.add(Box.createVerticalStrut(3));
                line(details, "HP " + difficulty.getMonsterHp() + " · "
                        + difficulty.getPlayerGravityMillis() + "ms", 11, UniversityPixelTheme.TEXT_SUB);
                line(details, "아이템 LV " + difficulty.getMonsterItemLevel(), 11, UniversityPixelTheme.TEXT_SUB);
                details.add(Box.createVerticalGlue());
                JLabel state = line(details, "잠김", 11, UniversityPixelTheme.TEXT_SUB);
                states.put(monster.getId(), state);
                card.add(details, BorderLayout.CENTER);
                JButton button = new PixelButton("도전하기"); button.setName(monster.getId());
                button.setPreferredSize(new Dimension(100, 34));
                encounters.put(monster.getId(), button); card.add(button, BorderLayout.SOUTH); cards.add(card);
            }
            chapter.add(cards, BorderLayout.CENTER); chapters.add(chapter);
        }
        add(chapters, BorderLayout.CENTER);
        JLabel foot = new JLabel("한 전투를 이기면 다음 레벨이 열립니다. 완료한 전투는 다시 도전할 수 있습니다.");
        foot.setForeground(UniversityPixelTheme.TEXT_SUB); add(foot, BorderLayout.SOUTH);
    }
    private static JLabel line(JPanel target, String text, int size, Color color) {
        JLabel label = new JLabel(text); label.setForeground(color);
        label.setFont(UniversityPixelTheme.font(size, Font.BOLD));
        label.setAlignmentX(Component.LEFT_ALIGNMENT); target.add(label); return label;
    }
    private static String introduction(String id) {
        if ("university".equals(id)) return "출석부터 교수님의 시험까지, 캠퍼스 생활을 통과하세요.";
        if ("graduation".equals(id)) return "주제 정하기 · 컴퓨터 AI · 재수강을 넘어 캡스톤 논문을 완성하세요.";
        return "코딩테스트와 면접을 통과하고 마지막 기업 전투에서 사원증을 획득하세요.";
    }
    private static String tierName(MonsterTier tier) {
        return tier == MonsterTier.NORMAL ? "일반" : tier == MonsterTier.ELITE ? "엘리트" : "보스";
    }
    public void updateProgress(CampaignProgress progress, StageCatalog stages) {
        int cleared = 0;
        for (Stage stage : stages.getStages()) for (MonsterSpec monster : stage.getEncounters()) {
            boolean unlocked = progress.isEncounterUnlocked(stage.getId(), monster.getId());
            boolean complete = progress.isEncounterCleared(monster.getId()); if (complete) cleared++;
            JButton button = encounters.get(monster.getId()); button.setEnabled(unlocked);
            button.setText(complete ? "다시 도전" : unlocked ? "도전하기" : "잠김");
            button.setBackground(complete ? UniversityPixelTheme.MINT : UniversityPixelTheme.GOLD);
            states.get(monster.getId()).setText(complete ? "CLEAR" : unlocked ? "도전 가능" : "이전 전투를 완료하세요");
        }
        completion.setText(cleared + " / 9 CLEAR");
    }
    public void setBackAction(ActionListener listener) { back.addActionListener(listener); }
    public void setStageAction(int stageIndex, String encounterId, ActionListener listener) {
        JButton button = encounters.get(encounterId); if (button != null) button.addActionListener(listener);
    }
    public void setStageAction(int stageIndex, MonsterTier tier, ActionListener listener) {
        for (MonsterSpec monster : catalog.getStages().get(stageIndex).getEncounters())
            if (monster.getTier() == tier) { setStageAction(stageIndex, monster.getId(), listener); return; }
    }
    public void updateProgress(StoryProgressData ignored) { }
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
