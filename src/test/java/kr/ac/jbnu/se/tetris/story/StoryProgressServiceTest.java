package kr.ac.jbnu.se.tetris.story;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import kr.ac.jbnu.se.tetris.battle.BattleManager;
import kr.ac.jbnu.se.tetris.battle.BattleResult;
import kr.ac.jbnu.se.tetris.battle.ParticipantSpec;

/** 실제 전투 결과에 따른 잠금, 재시도, 중복, 다음 전투 검증 */
public final class StoryProgressServiceTest {
    public static void main(String[] args) throws Exception {
        StageCatalog catalog = fixtureCatalog();
        StoryProgressService story = new StoryProgressService(catalog);
        CampaignProgress initial = story.getCampaignProgress();
        check(initial.isStageUnlocked("s1") && !initial.isStageUnlocked("s2"),
                "첫 스테이지만 해금");
        expectState(() -> story.startStage("s2", "locked", "p", "m"));
        check(initial.isEncounterUnlocked("s1", MonsterTier.NORMAL)
                && !initial.isEncounterUnlocked("s1", MonsterTier.ELITE), "원본 난이도 버튼의 초기 잠금");
        expectState(() -> story.startEncounter("s1", MonsterTier.ELITE, "locked-elite", "p", "m"));
        expectInvalid(() -> story.startStage("missing", "missing", "p", "m"));

        EncounterRun normal = story.startStage("s1", "run-1", "p", "m");
        check(normal.getEncounterId().equals("s1-normal") && !normal.isFinished(),
                "첫 일반 전투 시작");
        BattleManager pendingBattle = battle();
        BattleResult start = pendingBattle.start();
        check(story.recordBattleResult("run-1", start)
                == StoryProgressService.ResultDisposition.IN_PROGRESS, "진행 중 결과 제외");
        BattleResult win = pendingBattle.forfeit("m");
        check(story.recordBattleResult("run-1", win)
                == StoryProgressService.ResultDisposition.APPLIED_WIN, "실제 승리 반영");
        check(story.getActiveRun().isFinished() && story.getActiveRun().isWon(),
                "진행 기록의 결과 확정");
        check(story.recordBattleResult("run-1", win)
                == StoryProgressService.ResultDisposition.DUPLICATE, "중복 승리 무시");
        check(story.recordBattleResult("run-1", finishedForfeit("p"))
                == StoryProgressService.ResultDisposition.CONFLICT, "상충 결과 무시");
        check(!initial.isEncounterCleared("s1-normal")
                && story.getCampaignProgress().isEncounterCleared("s1-normal"),
                "이전 진행 스냅샷 불변");
        check(story.getCampaignProgress().isEncounterUnlocked("s1", MonsterTier.ELITE)
                && !story.getCampaignProgress().isEncounterUnlocked("s1", MonsterTier.BOSS), "일반 승리 후 엘리트만 해금");

        EncounterRun elite = story.startEncounter("s1", MonsterTier.ELITE, "run-2", "p", "m");
        check(elite.getEncounterId().equals("s1-elite"), "홈 재진입은 첫 미완료 전투");
        check(story.recordBattleResult("run-1", win)
                == StoryProgressService.ResultDisposition.STALE, "이전 run 결과 무시");
        check(story.recordBattleResult("run-2", finishedForfeit("p"))
                == StoryProgressService.ResultDisposition.APPLIED_LOSS, "패배 기록");
        check(!story.getCampaignProgress().isEncounterCleared("s1-elite"), "패배 시 미해금");
        expectState(() -> story.nextEncounter("invalid-next"));
        EncounterRun retry = story.restartActive("run-3");
        check(retry.getEncounterId().equals("s1-elite") && !retry.isFinished(),
                "패배 재시도는 새 run의 같은 전투");
        expectInvalid(() -> story.restartActive("run-2"));
        check(story.recordBattleResult("run-3", finishedForfeit("m"))
                == StoryProgressService.ResultDisposition.APPLIED_WIN, "재시도 승리");

        EncounterRun boss = story.nextEncounter("run-4");
        check(boss.getEncounterId().equals("s1-boss"), "엘리트 다음 보스");
        story.recordBattleResult("run-4", finishedForfeit("m"));
        check(story.getCampaignProgress().isStageCleared("s1")
                && story.getCampaignProgress().isStageUnlocked("s2"), "보스 승리로 다음 Stage 해금");
        EncounterRun nextStage = story.nextEncounter("run-5");
        check(nextStage.getStageId().equals("s2")
                && nextStage.getEncounterId().equals("s2-normal"), "다음 Stage 일반 전투");

        story.recordBattleResult("run-5", finishedForfeit("m"));
        story.recordBattleResult(story.nextEncounter("run-6").getRunId(), finishedForfeit("m"));
        story.recordBattleResult(story.nextEncounter("run-7").getRunId(), finishedForfeit("m"));
        check(story.getCampaignProgress().isStageCleared("s2"), "마지막 Stage 완료");
        check(story.nextEncounter("unused-final") == null, "최종 보스 뒤 다음 전투 없음");
        check(story.startStage("s1", "run-8", "p", "m").getEncounterId().equals("s1-normal"),
                "완료 Stage 재플레이는 일반 전투부터");
        check(story.startEncounter("s1", MonsterTier.BOSS, "replay-boss", "p", "m").getEncounterId().equals("s1-boss"),
                "해금된 보스 버튼의 직접 재도전");
    }

    private static BattleManager battle() {
        return new BattleManager(Arrays.asList(new ParticipantSpec("p", "PLAYER", 100),
                new ParticipantSpec("m", "MONSTER", 100)), 17L);
    }

    private static BattleResult finishedForfeit(String actorId) {
        BattleManager battle = battle();
        check(battle.start().isAccepted(), "대전 시작");
        BattleResult result = battle.forfeit(actorId);
        check(result.isAccepted(), "전투 포기 결과 생성");
        return result;
    }

    private static StageCatalog fixtureCatalog() throws IOException {
        StringBuilder text = new StringBuilder("stage.count=2\n");
        for (int stage = 1; stage <= 2; stage++) {
            text.append("stage.").append(stage).append(".id=s").append(stage).append('\n');
            text.append("stage.").append(stage).append(".name=Stage ").append(stage).append('\n');
            for (String tier : new String[] { "normal", "elite", "boss" }) {
                String key = "stage." + stage + "." + tier;
                text.append(key).append(".id=s").append(stage).append('-').append(tier).append('\n');
                text.append(key).append(".name=Monster ").append(tier).append('\n');
                text.append(key).append(".hp=100\n");
                text.append(key).append(".aiProfile=").append(tier).append("_default\n");
            }
        }
        return StageCatalog.load(new StringReader(text.toString()));
    }

    private static void expectState(Runnable action) {
        try { action.run(); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("잘못된 상태 전이를 거부해야 한다");
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("잘못된 ID를 거부해야 한다");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
