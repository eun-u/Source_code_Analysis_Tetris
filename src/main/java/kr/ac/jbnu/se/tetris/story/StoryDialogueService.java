package kr.ac.jbnu.se.tetris.story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 전투 상태와 확정 진행 결과를 화면용 대사 이벤트로 바꾼다. */
public final class StoryDialogueService {
    private static final int MONSTER_HP_PERCENT = 50;

    private final StageCatalog catalog;
    private final Set<String> hpTriggeredRuns = new HashSet<String>();
    private final Set<String> resultTriggeredRuns = new HashSet<String>();

    public StoryDialogueService(StageCatalog catalog) {
        if (catalog == null) throw new IllegalArgumentException("Stage catalog is required");
        this.catalog = catalog;
    }

    /** 진행 중인 몬스터 HP가 기준 이하로 처음 내려간 순간 한 번만 발화한다. */
    public synchronized StoryDialogueCue onMonsterHp(EncounterRun run, int hp, int maxHp) {
        requireRun(run);
        if (maxHp <= 0 || hp < 0 || hp > maxHp) throw new IllegalArgumentException("Invalid monster HP");
        if (run.isFinished() || hp == 0 || (long) hp * 100 > (long) maxHp * MONSTER_HP_PERCENT
                || !hpTriggeredRuns.add(run.getRunId())) return null;
        return cue(StoryDialogueCue.Trigger.MONSTER_HP_THRESHOLD, run,
                hpSpeaker(run), hpLine(run));
    }

    /** recordBattleResult 직전/직후의 진행 스냅샷으로 최초 해금 이벤트를 구별한다. */
    public synchronized List<StoryDialogueCue> onResult(EncounterRun run,
            StoryProgressService.ResultDisposition disposition,
            CampaignProgress before, CampaignProgress after) {
        requireRun(run);
        if (disposition == null || before == null || after == null) {
            throw new IllegalArgumentException("Disposition and campaign progress are required");
        }
        if (disposition != StoryProgressService.ResultDisposition.APPLIED_WIN
                && disposition != StoryProgressService.ResultDisposition.APPLIED_LOSS) {
            return Collections.emptyList();
        }
        if (!run.isFinished()
                || run.isWon() != (disposition == StoryProgressService.ResultDisposition.APPLIED_WIN)
                || !resultTriggeredRuns.add(run.getRunId())) {
            return Collections.emptyList();
        }

        List<StoryDialogueCue> cues = new ArrayList<StoryDialogueCue>();
        if (disposition == StoryProgressService.ResultDisposition.APPLIED_LOSS) {
            cues.add(cue(StoryDialogueCue.Trigger.ENCOUNTER_LOSS, run,
                    lossSpeaker(run), lossLine(run)));
            return Collections.unmodifiableList(cues);
        }

        cues.add(cue(StoryDialogueCue.Trigger.ENCOUNTER_WIN, run,
                winSpeaker(run), winLine(run)));
        boolean firstClear = !before.isEncounterCleared(run.getEncounterId())
                && after.isEncounterCleared(run.getEncounterId());
        if (firstClear) {
            int midpoint = (encounterCount() + 1) / 2;
            if (before.getCompletedEncounterIds().size() < midpoint
                    && after.getCompletedEncounterIds().size() >= midpoint) {
                cues.add(cue(StoryDialogueCue.Trigger.HALFWAY, run,
                        "내레이션", "여기까지는 준비 운동이었다. 이제부터 시작이다!"));
            }
            if (!before.isStageCleared(run.getStageId()) && after.isStageCleared(run.getStageId())) {
                cues.add(cue(StoryDialogueCue.Trigger.STAGE_CLEAR, run,
                        "내레이션", stageClearLine(run)));
            }
            if (after.getCompletedEncounterIds().size() == encounterCount()) {
                cues.add(cue(StoryDialogueCue.Trigger.CAMPAIGN_CLEAR, run,
                        "내레이션", "마침내 취업에 성공했다! 끝없이 쌓인 과제와 면접을 넘어, 첫 출근의 문이 열렸다!"));
            }
        }
        return Collections.unmodifiableList(cues);
    }

    private void requireRun(EncounterRun run) {
        if (run == null || catalog.getStage(run.getStageId()) == null) {
            throw new IllegalArgumentException("Known encounter run is required");
        }
    }

    private int encounterCount() {
        int count = 0;
        for (Stage stage : catalog.getStages()) count += stage.getEncounters().size();
        return count;
    }

    private int stageNumber(EncounterRun run) {
        for (int i = 0; i < catalog.getStages().size(); i++) {
            if (catalog.getStages().get(i).getId().equals(run.getStageId())) return i + 1;
        }
        throw new IllegalArgumentException("Unknown story stage");
    }

    private static StoryDialogueCue cue(StoryDialogueCue.Trigger trigger,
            EncounterRun run, String speaker, String line) {
        return new StoryDialogueCue(trigger, run, speaker, line);
    }

    // 기획·프론트엔드 담당자는 트리거 횟수, HP 기준과 타이밍, 대사 문구·화자,
    // 화면 배치·디자인을 모두 자유롭게 바꾸어도 된다. 아래 문구는 연결용 예시다.
    private static String hpSpeaker(EncounterRun run) {
        return "level_4_monster".equals(run.getEncounterId()) ? "플레이어" : "몬스터";
    }

    private static String hpLine(EncounterRun run) {
        String id = run.getEncounterId();
        if ("level_3_monster".equals(id)) return "오늘은 오픈북이 아니다 으하하!";
        if ("level_4_monster".equals(id)) return "벼락치기의 힘!~으어어!";
        if ("level_8_monster".equals(id)) return "지원동기를 말해라 이녀석!!!!!";
        return "이 정도면 제법이군. 하지만 아직 끝나지 않았다!";
    }

    private static String winSpeaker(EncounterRun run) {
        return "level_3_monster".equals(run.getEncounterId()) ? "몬스터" : "플레이어";
    }

    private static String winLine(EncounterRun run) {
        if ("level_3_monster".equals(run.getEncounterId())) {
            return "이녀석 제법이구만… 대학원에 들어오지 않겠나?";
        }
        return "후….이번 학기는 살았다…";
    }

    private static String lossSpeaker(EncounterRun run) {
        return "level_4_monster".equals(run.getEncounterId())
                || "level_8_monster".equals(run.getEncounterId()) ? "몬스터" : "내레이션";
    }

    private String lossLine(EncounterRun run) {
        if ("level_4_monster".equals(run.getEncounterId())) return "재수강을 하려고 이려냐!!!!";
        if ("level_8_monster".equals(run.getEncounterId())) return "넌 면접 점수 0점이다. ㅋ";
        if (stageNumber(run) == 2) return "졸업에 실패했다. 취업의 문은 아직 멀고 높았다…";
        if (stageNumber(run) == 3) return "취업의 문은 높았다. 다음 면접에서 다시 도전하자…";
        return "이번 학기는 여기까지인가… 다시 도전하자!";
    }

    private String stageClearLine(EncounterRun run) {
        if (stageNumber(run) == 1) return "대학의 첫 관문을 넘었다! 다음 학기가 기다린다.";
        if (stageNumber(run) == 2) return "마침내 졸업이다! 이제 취업의 문을 두드릴 차례다.";
        return "취업 과정의 마지막 관문까지 돌파했다!";
    }
}
