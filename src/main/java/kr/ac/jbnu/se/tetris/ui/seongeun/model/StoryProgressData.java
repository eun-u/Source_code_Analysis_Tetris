package kr.ac.jbnu.se.tetris.ui.seongeun.model;

public class StoryProgressData {

    public static final int NORMAL = 0;
    public static final int ELITE = 1;
    public static final int BOSS = 2;

    private int highestUnlockedIndex;

    public StoryProgressData(int highestUnlockedIndex) {
        this.highestUnlockedIndex = highestUnlockedIndex;
    }

    public boolean isUnlocked(int stage, int difficulty) {
        int index = getStageIndex(stage, difficulty);
        return index <= highestUnlockedIndex;
    }

    public void clearStage(int stage, int difficulty) {
        int clearedIndex = getStageIndex(stage, difficulty);

        if (clearedIndex == highestUnlockedIndex && highestUnlockedIndex < 8) {
            highestUnlockedIndex++;
        }
    }

    public int getHighestUnlockedIndex() {
        return highestUnlockedIndex;
    }

    private int getStageIndex(int stage, int difficulty) {
        return (stage - 1) * 3 + difficulty;
    }
}
