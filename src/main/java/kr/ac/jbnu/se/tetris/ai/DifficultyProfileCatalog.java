package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

/** LEVEL_1..9의 공통 프리셋과 기존 스토리 몬스터 HP 보존 매핑. */
public final class DifficultyProfileCatalog {
    private static final int[] HP = {65, 75, 90, 100, 115, 125, 140, 150, 165};
    private static final int[] GRAVITY = {550, 525, 500, 475, 450, 425, 400, 375, 350};
    private static final int[] DELAY = {1800, 1650, 1500, 1350, 1200, 1100, 1000, 900, 800};
    private static final int[] STATES = {500, 650, 800, 1000, 1200, 1400, 1700, 2000, 2300};
    private static final int[] BUDGET = {30, 35, 40, 50, 60, 70, 80, 95, 110};
    private static final double[] DAMAGE = {0, 0, 0.05, 0.05, 0.1, 0.1, 0.15, 0.2, 0.25};
    private DifficultyProfileCatalog() { }
    public static DifficultyProfile level(int level) {
        if (level < 1 || level > 9) throw new IllegalArgumentException("Level must be 1..9");
        int i = level - 1;
        DifficultyProfile.SpecialPattern pattern = level <= 3 ? DifficultyProfile.SpecialPattern.FIXED
                : level <= 6 ? DifficultyProfile.SpecialPattern.ADAPTIVE
                : DifficultyProfile.SpecialPattern.BOSS_PHASE;
        String profileId = level <= 3 ? "normal_default" : level <= 6 ? "elite_default" : "boss_default";
        return new DifficultyProfile(level, HP[i], GRAVITY[i], DELAY[i],
                new DifficultyProfile.AiStrength(STATES[i], BUDGET[i]),
                new DifficultyProfile.AttackStrength(DAMAGE[i], level >= 8 ? 1 : 0),
                pattern, profileId);
    }
    public static DifficultyProfile forEncounter(MonsterSpec monster) {
        if (monster == null) throw new IllegalArgumentException("Monster required");
        StageCatalog catalog = StageCatalog.loadDefault();
        int stageIndex = 0;
        for (Stage stage : catalog.getStages()) {
            stageIndex++;
            for (MonsterSpec candidate : stage.getEncounters()) {
                if (candidate.getId().equals(monster.getId())) {
                    int[] base = {1, 2, 3, 5, 7};
                    int level = Math.min(9, base[Math.min(base.length - 1, stageIndex - 1)]
                            + monster.getTier().ordinal());
                    DifficultyProfile.SpecialPattern pattern = monster.getTier() == MonsterTier.NORMAL
                            ? DifficultyProfile.SpecialPattern.FIXED
                            : monster.getTier() == MonsterTier.ELITE
                            ? DifficultyProfile.SpecialPattern.ADAPTIVE
                            : DifficultyProfile.SpecialPattern.BOSS_PHASE;
                    return level(level).withEncounter(monster.getHp(), pattern, monster.getAiProfileId());
                }
            }
        }
        throw new IllegalArgumentException("Unknown story monster: " + monster.getId());
    }
}
