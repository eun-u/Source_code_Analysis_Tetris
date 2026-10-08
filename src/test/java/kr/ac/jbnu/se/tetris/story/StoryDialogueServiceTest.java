package kr.ac.jbnu.se.tetris.story;

import java.util.Arrays;
import java.util.List;
import kr.ac.jbnu.se.tetris.battle.BattleManager;
import kr.ac.jbnu.se.tetris.battle.BattleResult;
import kr.ac.jbnu.se.tetris.battle.ParticipantSpec;

/** 실제 9전투 진행과 재시도에서 대사 트리거의 시점·중복 여부 검증. */
public final class StoryDialogueServiceTest {
    public static void main(String[] args) {
        hpThresholdAndRetry();
        campaignMilestonesAndReplay();
    }

    private static void hpThresholdAndRetry() {
        StageCatalog catalog = StageCatalog.loadDefault();
        StoryProgressService progress = new StoryProgressService(catalog);
        StoryDialogueService dialogue = new StoryDialogueService(catalog);
        EncounterRun first = progress.startStage("university", "first", "p", "m");

        check(dialogue.onMonsterHp(first, 51, 100) == null, "HP 절반 전에는 발화하지 않음");
        StoryDialogueCue hpCue = dialogue.onMonsterHp(first, 50, 100);
        check(hpCue != null && hpCue.getTrigger() == StoryDialogueCue.Trigger.MONSTER_HP_THRESHOLD,
                "HP 절반 도달 시 한 번 발화");
        check("first".equals(hpCue.getRunId()) && first.getEncounterId().equals(hpCue.getEncounterId()),
                "대사는 실제 전투와 몬스터를 식별함");
        check(dialogue.onMonsterHp(first, 30, 100) == null
                && dialogue.onMonsterHp(first, 50, 100) == null,
                "추가 피해와 회복 뒤 같은 전투에서는 반복하지 않음");

        CampaignProgress beforeLoss = progress.getCampaignProgress();
        StoryProgressService.ResultDisposition loss = progress.recordBattleResult("first", forfeit("p"));
        List<StoryDialogueCue> lossCues = dialogue.onResult(progress.getActiveRun(), loss,
                beforeLoss, progress.getCampaignProgress());
        check(lossCues.size() == 1 && lossCues.get(0).getTrigger() == StoryDialogueCue.Trigger.ENCOUNTER_LOSS,
                "패배 이벤트는 1건이고 진행을 해금하지 않음");
        check(progress.getCampaignProgress().getCompletedEncounterIds().isEmpty(), "패배는 미클리어");
        check(dialogue.onMonsterHp(progress.getActiveRun(), 20, 100) == null,
                "종료된 전투에서는 HP 대사를 발화하지 않음");
        check(dialogue.onResult(progress.getActiveRun(), loss, beforeLoss,
                progress.getCampaignProgress()).isEmpty(), "결과 콜백 재호출은 중복 대사 없음");

        EncounterRun retry = progress.restartActive("retry");
        check(dialogue.onMonsterHp(retry, 50, 100) != null, "새 전투에서는 HP 대사를 다시 허용");
        check(dialogue.onResult(retry, StoryProgressService.ResultDisposition.IN_PROGRESS,
                beforeLoss, beforeLoss).isEmpty(), "진행 중 결과는 대사 없음");
        expectInvalid(() -> dialogue.onMonsterHp(retry, 101, 100));
    }

    private static void campaignMilestonesAndReplay() {
        StageCatalog catalog = StageCatalog.loadDefault();
        StoryProgressService progress = new StoryProgressService(catalog);
        StoryDialogueService dialogue = new StoryDialogueService(catalog);
        int encounter = 0;
        for (Stage stage : catalog.getStages()) {
            for (MonsterSpec monster : stage.getEncounters()) {
                encounter++;
                EncounterRun run = progress.startEncounter(stage.getId(), monster.getId(),
                        "win-" + encounter, "p", "m");
                CampaignProgress before = progress.getCampaignProgress();
                StoryProgressService.ResultDisposition disposition =
                        progress.recordBattleResult(run.getRunId(), forfeit("m"));
                CampaignProgress after = progress.getCampaignProgress();
                List<StoryDialogueCue> cues = dialogue.onResult(progress.getActiveRun(), disposition,
                        before, after);
                check(has(cues, StoryDialogueCue.Trigger.ENCOUNTER_WIN), "매 전투 승리 발화");
                check(has(cues, StoryDialogueCue.Trigger.STAGE_CLEAR) == (encounter % 3 == 0),
                        "3/6/9번째 전투에서 과정 완료 발화");
                check(has(cues, StoryDialogueCue.Trigger.HALFWAY) == (encounter == 5),
                        "전체 진행의 절반을 넘는 5번째 전투에서 한 번 발화");
                check(has(cues, StoryDialogueCue.Trigger.CAMPAIGN_CLEAR) == (encounter == 9),
                        "최종 전투 승리에서만 전체 완료 발화");
                check(dialogue.onResult(progress.getActiveRun(), disposition, before, after).isEmpty(),
                        "동일 결과 콜백은 재발화하지 않음");
            }
        }

        EncounterRun replay = progress.startEncounter("university", "level_3_monster",
                "replay", "p", "m");
        CampaignProgress beforeReplay = progress.getCampaignProgress();
        StoryProgressService.ResultDisposition disposition =
                progress.recordBattleResult(replay.getRunId(), forfeit("m"));
        List<StoryDialogueCue> replayCues = dialogue.onResult(progress.getActiveRun(), disposition,
                beforeReplay, progress.getCampaignProgress());
        check(replayCues.size() == 1
                && replayCues.get(0).getTrigger() == StoryDialogueCue.Trigger.ENCOUNTER_WIN,
                "클리어한 전투 재플레이는 신규 과정/전체 완료를 다시 띄우지 않음");
    }

    private static boolean has(List<StoryDialogueCue> cues, StoryDialogueCue.Trigger trigger) {
        for (StoryDialogueCue cue : cues) if (cue.getTrigger() == trigger) return true;
        return false;
    }

    private static BattleResult forfeit(String actorId) {
        BattleManager battle = new BattleManager(Arrays.asList(
                new ParticipantSpec("p", "PLAYER", 100),
                new ParticipantSpec("m", "MONSTER", 100)), 17L);
        check(battle.start().isAccepted(), "전투 시작");
        BattleResult result = battle.forfeit(actorId);
        check(result.isAccepted(), "전투 종료");
        return result;
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("잘못된 HP를 거부해야 한다");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
