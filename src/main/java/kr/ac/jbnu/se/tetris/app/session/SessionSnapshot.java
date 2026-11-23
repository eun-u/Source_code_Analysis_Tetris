package kr.ac.jbnu.se.tetris.app.session;

import kr.ac.jbnu.se.tetris.battle.BattleSnapshots;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.network.RoomState;

/** 대전 모드와 참가자 표시 상태의 불변 사본 */
public final class SessionSnapshot {
    private final String sessionId;
    private final SessionMode mode;
    private final SessionPhase phase;
    private final String localParticipantId;
    private final String matchId;
    private final BattleState battleState;
    private final SessionCapabilities capabilities;
    private final RoomState roomState;

    public SessionSnapshot(String sessionId, SessionMode mode, SessionPhase phase,
                           String localParticipantId, String matchId, BattleState battleState,
                           SessionCapabilities capabilities) {
        this(sessionId, mode, phase, localParticipantId, matchId, battleState, capabilities, null);
    }

    public SessionSnapshot(String sessionId, SessionMode mode, SessionPhase phase,
                           String localParticipantId, String matchId, BattleState battleState,
                           SessionCapabilities capabilities, RoomState roomState) {
        if (blank(sessionId) || mode == null || phase == null || capabilities == null
                || (matchId != null && blank(matchId))
                || (localParticipantId != null && blank(localParticipantId))
                || (roomState != null && (mode != SessionMode.ONLINE_PVP
                        || !roomState.getLocalParticipantId().equals(localParticipantId)))) {
            throw new IllegalArgumentException("Invalid session snapshot");
        }
        BattleState.Status expected = expectedBattleStatus(phase);
        if (expected != null) {
            if (battleState == null || blank(matchId) || blank(localParticipantId)
                    || battleState.getParticipant(localParticipantId) == null) {
                throw new IllegalArgumentException("Active session needs a local battle participant");
            }
            if (battleState.getStatus() != expected) {
                throw new IllegalArgumentException("Session phase and battle status disagree");
            }
        } else if (battleState != null) {
            throw new IllegalArgumentException("Inactive session cannot carry a battle state");
        }
        this.sessionId = sessionId;
        this.mode = mode;
        this.phase = phase;
        this.localParticipantId = localParticipantId;
        this.matchId = matchId;
        this.battleState = battleState == null ? null : BattleSnapshots.copyOf(battleState);
        this.capabilities = capabilities;
        this.roomState = roomState;
    }

    /** 전투를 가진 단계의 기대 전투 상태. 대기·연결 단계처럼 전투가 없는 단계는 null이다. */
    private static BattleState.Status expectedBattleStatus(SessionPhase phase) {
        switch (phase) {
            case RUNNING: return BattleState.Status.RUNNING;
            case PAUSED: return BattleState.Status.PAUSED;
            case FINISHED: return BattleState.Status.FINISHED;
            default: return null;
        }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    public String getSessionId() { return sessionId; }
    public SessionMode getMode() { return mode; }
    public SessionPhase getPhase() { return phase; }
    public String getLocalParticipantId() { return localParticipantId; }
    public String getMatchId() { return matchId; }
    public BattleState getBattleState() { return battleState; }
    public SessionCapabilities getCapabilities() { return capabilities; }
    public RoomState getRoomState() { return roomState; }
}
