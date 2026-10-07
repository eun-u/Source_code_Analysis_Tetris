package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

/** 패턴과 별도로 HP, 속도, 공격, 아이템을 정한 아홉 레벨 프리셋. */
public final class DifficultyProfileCatalog {
    private static final StageCatalog CAMPAIGN = StageCatalog.loadDefault();
    private static final int[] GRAVITY = {450, 420, 400, 370, 350, 330, 310, 290, 270};
    private static final int[] DELAY = {3500, 3300, 3000, 2800, 2600, 2450, 2300, 2150, 2000};
    private static final int[] STATES = {150, 150, 400, 400, 400, 800, 800, 800, 800};
    private static final int[] BUDGET = {12, 12, 24, 24, 24, 40, 40, 40, 40};
    private static final int[] ITEM_LEVEL = {0, 0, 0, 1, 2, 2, 3, 4, 5};
    private static final int[] AI_TIER = {1, 1, 2, 2, 2, 3, 3, 3, 3};
    private static final double[] ATTACK_MULTIPLIER =
            {1.00, 1.05, 1.10, 1.15, 1.20, 1.25, 1.30, 1.40, 1.50};
    private static final MonsterTier[] PATTERN = {
        MonsterTier.NORMAL, MonsterTier.ELITE, MonsterTier.BOSS,
        MonsterTier.NORMAL, MonsterTier.ELITE, MonsterTier.BOSS,
        MonsterTier.ELITE, MonsterTier.ELITE, MonsterTier.BOSS
    };
    private DifficultyProfileCatalog() { }

    public static int getAiTier(int level) { return AI_TIER[index(level)]; }
    public static MonsterTier getPattern(int level) { return PATTERN[index(level)]; }
    public static double getAttackMultiplier(int level) { return ATTACK_MULTIPLIER[index(level)]; }

    public static DifficultyProfile level(int level) {
        int i = index(level);
        return new DifficultyProfile(level, encounterAt(level).getHp(), GRAVITY[i], DELAY[i],
                new DifficultyProfile.AiStrength(STATES[i], BUDGET[i]),
                new DifficultyProfile.AttackStrength(ATTACK_MULTIPLIER[i] - 1.0, 0),
                special(PATTERN[i]), profileId(PATTERN[i]), ITEM_LEVEL[i]);
    }

    public static DifficultyProfile forEncounter(MonsterSpec monster) {
        if (monster == null) throw new IllegalArgumentException("Monster required");
        int level = 0;
        for (Stage stage : CAMPAIGN.getStages()) {
            for (MonsterSpec candidate : stage.getEncounters()) {
                level++;
                if (candidate.getId().equals(monster.getId())) {
                    if (candidate.getTier() != getPattern(level))
                        throw new IllegalStateException("Campaign pattern differs from level " + level);
                    DifficultyProfile base = level(level);
                    return base.withEncounter(candidate.getHp(), special(candidate.getTier()),
                            candidate.getAiProfileId());
                }
            }
        }
        throw new IllegalArgumentException("Unknown story monster: " + monster.getId());
    }

    private static int index(int level) {
        if (level < 1 || level > 9) throw new IllegalArgumentException("Level must be 1..9");
        return level - 1;
    }
    private static MonsterSpec encounterAt(int level) {
        int current = 0;
        for (Stage stage : CAMPAIGN.getStages()) {
            for (MonsterSpec monster : stage.getEncounters()) {
                if (++current == level) return monster;
            }
        }
        throw new IllegalStateException("Missing campaign encounter for level " + level);
    }
    private static DifficultyProfile.SpecialPattern special(MonsterTier tier) {
        return tier == MonsterTier.NORMAL ? DifficultyProfile.SpecialPattern.FIXED
                : tier == MonsterTier.ELITE ? DifficultyProfile.SpecialPattern.ADAPTIVE
                : DifficultyProfile.SpecialPattern.BOSS_PHASE;
    }
    private static String profileId(MonsterTier tier) {
        return tier.name().toLowerCase(java.util.Locale.ROOT) + "_default";
    }
}
