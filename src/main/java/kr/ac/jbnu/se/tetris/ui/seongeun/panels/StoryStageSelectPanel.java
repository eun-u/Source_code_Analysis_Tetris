package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.StoryProgressData;
import kr.ac.jbnu.se.tetris.story.CampaignProgress;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

public class StoryStageSelectPanel extends JPanel {

    private GameButton backButton;

    private GameButton stage1NormalButton;
    private GameButton stage1EliteButton;
    private GameButton stage1BossButton;

    private GameButton stage2NormalButton;
    private GameButton stage2EliteButton;
    private GameButton stage2BossButton;

    private GameButton stage3NormalButton;
    private GameButton stage3EliteButton;
    private GameButton stage3BossButton;

    public StoryStageSelectPanel() {
        setLayout(new BorderLayout());

        // 상단 영역
        JLabel titleLabel = new JLabel("Story");
        backButton = new GameButton("Back");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(backButton, BorderLayout.EAST);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(topPanel, BorderLayout.CENTER);
        headerPanel.add(new JSeparator(), BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Stage 전체 영역
        JPanel stageContainer = new JPanel();
        stageContainer.setLayout(new BoxLayout(stageContainer, BoxLayout.Y_AXIS));
        stageContainer.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Stage 1
        JPanel stage1Panel = createStagePanel(
            "Stage 1",
            stage1NormalButton = new GameButton("일반"),
            stage1EliteButton = new GameButton("엘리트"),
            stage1BossButton = new GameButton("보스")
        );

        // Stage 2
        JPanel stage2Panel = createStagePanel(
            "Stage 2",
            stage2NormalButton = new GameButton("일반"),
            stage2EliteButton = new GameButton("엘리트"),
            stage2BossButton = new GameButton("보스")
        );

        // Stage 3
        JPanel stage3Panel = createStagePanel(
            "Stage 3",
            stage3NormalButton = new GameButton("일반"),
            stage3EliteButton = new GameButton("엘리트"),
            stage3BossButton = new GameButton("보스")
        );

        stageContainer.add(stage1Panel);
        stageContainer.add(Box.createVerticalStrut(15));
        stageContainer.add(stage2Panel);
        stageContainer.add(Box.createVerticalStrut(15));
        stageContainer.add(stage3Panel);

        add(stageContainer, BorderLayout.CENTER);
    }

    private JPanel createStagePanel(String stageName, GameButton normalButton, GameButton eliteButton, GameButton bossButton) {
        JLabel stageLabel = new JLabel(stageName);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        buttonPanel.add(normalButton);
        buttonPanel.add(eliteButton);
        buttonPanel.add(bossButton);

        JPanel stagePanel = new JPanel(new BorderLayout(0, 5));
        stagePanel.add(stageLabel, BorderLayout.NORTH);
        stagePanel.add(buttonPanel, BorderLayout.CENTER);

        return stagePanel;
    }

    public void updateProgress(StoryProgressData progress) {
        updateButton(stage1NormalButton, "일반", progress.isUnlocked(1, StoryProgressData.NORMAL));
        updateButton(stage1EliteButton, "엘리트", progress.isUnlocked(1, StoryProgressData.ELITE));
        updateButton(stage1BossButton, "보스", progress.isUnlocked(1, StoryProgressData.BOSS));

        updateButton(stage2NormalButton, "일반", progress.isUnlocked(2, StoryProgressData.NORMAL));
        updateButton(stage2EliteButton, "엘리트", progress.isUnlocked(2, StoryProgressData.ELITE));
        updateButton(stage2BossButton, "보스", progress.isUnlocked(2, StoryProgressData.BOSS));

        updateButton(stage3NormalButton, "일반", progress.isUnlocked(3, StoryProgressData.NORMAL));
        updateButton(stage3EliteButton, "엘리트", progress.isUnlocked(3, StoryProgressData.ELITE));
        updateButton(stage3BossButton, "보스", progress.isUnlocked(3, StoryProgressData.BOSS));
    }

    /** 원본의 Stage 1~3 버튼을 현재 캠페인 진행 상태에 연결한다. */
    public void updateProgress(CampaignProgress progress, StageCatalog catalog) {
        if (progress == null || catalog == null || catalog.getStages().size() < 3)
            throw new IllegalArgumentException("Three stages and progress are required");
        String first = catalog.getStages().get(0).getId();
        String second = catalog.getStages().get(1).getId();
        String third = catalog.getStages().get(2).getId();
        updateButton(stage1NormalButton, "일반", progress.isEncounterUnlocked(first, MonsterTier.NORMAL));
        updateButton(stage1EliteButton, "엘리트", progress.isEncounterUnlocked(first, MonsterTier.ELITE));
        updateButton(stage1BossButton, "보스", progress.isEncounterUnlocked(first, MonsterTier.BOSS));
        updateButton(stage2NormalButton, "일반", progress.isEncounterUnlocked(second, MonsterTier.NORMAL));
        updateButton(stage2EliteButton, "엘리트", progress.isEncounterUnlocked(second, MonsterTier.ELITE));
        updateButton(stage2BossButton, "보스", progress.isEncounterUnlocked(second, MonsterTier.BOSS));
        updateButton(stage3NormalButton, "일반", progress.isEncounterUnlocked(third, MonsterTier.NORMAL));
        updateButton(stage3EliteButton, "엘리트", progress.isEncounterUnlocked(third, MonsterTier.ELITE));
        updateButton(stage3BossButton, "보스", progress.isEncounterUnlocked(third, MonsterTier.BOSS));
    }

    private void updateButton(GameButton button, String difficultyName, boolean unlocked) {
        button.setEnabled(unlocked);
        button.setText(unlocked ? difficultyName : "Locked");
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void setStage1NormalAction(ActionListener listener) {
        stage1NormalButton.addActionListener(listener);
    }

    public void setStage1EliteAction(ActionListener listener) {
        stage1EliteButton.addActionListener(listener);
    }

    public void setStage1BossAction(ActionListener listener) {
        stage1BossButton.addActionListener(listener);
    }

    public void setStage2NormalAction(ActionListener listener) {
        stage2NormalButton.addActionListener(listener);
    }

    public void setStage2EliteAction(ActionListener listener) {
        stage2EliteButton.addActionListener(listener);
    }

    public void setStage2BossAction(ActionListener listener) {
        stage2BossButton.addActionListener(listener);
    }

    public void setStage3NormalAction(ActionListener listener) {
        stage3NormalButton.addActionListener(listener);
    }

    public void setStage3EliteAction(ActionListener listener) {
        stage3EliteButton.addActionListener(listener);
    }

    public void setStage3BossAction(ActionListener listener) {
        stage3BossButton.addActionListener(listener);
    }
}
