package kr.ac.jbnu.se.tetris.story;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 전투 완료와 스테이지 해금을 보여 주는 불변 진행 기록 */
public final class CampaignProgress {
    private final StageCatalog catalog;
    private final Set<String> completedEncounterIds;

    CampaignProgress(StageCatalog catalog, Set<String> completedEncounterIds) {
        this.catalog = catalog;
        this.completedEncounterIds = Collections.unmodifiableSet(
                new LinkedHashSet<String>(completedEncounterIds));
    }

    public Set<String> getCompletedEncounterIds() { return completedEncounterIds; }

    public boolean isEncounterCleared(String encounterId) {
        return completedEncounterIds.contains(encounterId);
    }

    public boolean isStageUnlocked(String stageId) {
        int index = indexOf(stageId);
        if (index == 0) return true;
        Stage previous = catalog.getStages().get(index - 1);
        return completedEncounterIds.contains(previous.getEncounters().get(2).getId());
    }

    public boolean isStageCleared(String stageId) {
        Stage stage = catalog.getStages().get(indexOf(stageId));
        for (MonsterSpec encounter : stage.getEncounters()) {
            if (!completedEncounterIds.contains(encounter.getId())) return false;
        }
        return true;
    }

    /** 완료된 스테이지의 재진입은 첫 일반 전투 */
    public MonsterSpec getNextEncounter(String stageId) {
        Stage stage = catalog.getStages().get(indexOf(stageId));
        for (MonsterSpec encounter : stage.getEncounters()) {
            if (!completedEncounterIds.contains(encounter.getId())) return encounter;
        }
        return stage.getEncounters().get(0);
    }

    private int indexOf(String stageId) {
        if (stageId == null) throw new IllegalArgumentException("Stage ID is required");
        for (int index = 0; index < catalog.getStages().size(); index++) {
            if (catalog.getStages().get(index).getId().equals(stageId)) return index;
        }
        throw new IllegalArgumentException("Unknown stage ID: " + stageId);
    }
}
