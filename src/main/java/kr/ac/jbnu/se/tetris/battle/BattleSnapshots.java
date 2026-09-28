package kr.ac.jbnu.se.tetris.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.jbnu.se.tetris.core.CoreSnapshots;
import kr.ac.jbnu.se.tetris.core.GameState;

/** 네트워크 표시와 독립 UI 예제를 위한 전투 사본 생성 경계 */
public final class BattleSnapshots {
    private BattleSnapshots() { }

    public static ParticipantState participant(String id, String name, int hp, int maxHp,
            GameState state, boolean eliminated) {
        if (id == null || id.trim().isEmpty() || id.length() > 128 || name == null
                || name.trim().isEmpty() || name.length() > 128 || maxHp <= 0 || hp < 0 || hp > maxHp
                || state == null || !id.equals(state.getActorId())
                || ((hp == 0 || state.getStatus() == GameState.Status.GAME_OVER) && !eliminated)) {
            throw new IllegalArgumentException("Invalid participant snapshot");
        }
        return new ParticipantState(id, name, hp, maxHp, CoreSnapshots.copyOf(state), eliminated);
    }

    public static BattleState battle(BattleState.Status status, long version,
            Map<String, ParticipantState> participants, String winnerId, String reason) {
        if (status == null || version < 0 || participants == null || participants.size() < 2
                || participants.size() > 4) throw new IllegalArgumentException("Invalid battle snapshot");
        Map<String, ParticipantState> copied = new LinkedHashMap<String, ParticipantState>();
        for (Map.Entry<String, ParticipantState> entry : participants.entrySet()) {
            ParticipantState p = entry.getValue();
            if (p == null || !p.getId().equals(entry.getKey())) {
                throw new IllegalArgumentException("Participant key mismatch");
            }
            copied.put(entry.getKey(), participant(p.getId(), p.getName(), p.getHp(), p.getMaxHp(),
                    p.getGameState(), p.isEliminated()));
        }
        if (status != BattleState.Status.FINISHED && (winnerId != null || reason != null)) {
            throw new IllegalArgumentException("Unfinished battle cannot have a result");
        }
        if (status == BattleState.Status.FINISHED && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("Finished battle needs a reason");
        }
        if (winnerId != null && (!copied.containsKey(winnerId) || copied.get(winnerId).isEliminated())) {
            throw new IllegalArgumentException("Invalid winner");
        }
        return new BattleState(status, version, copied, winnerId, reason);
    }

    public static BattleState copyOf(BattleState state) {
        if (state == null) throw new IllegalArgumentException("Battle state is required");
        return battle(state.getStatus(), state.getVersion(), state.getParticipants(),
                state.getWinnerId(), state.getReason());
    }
}
