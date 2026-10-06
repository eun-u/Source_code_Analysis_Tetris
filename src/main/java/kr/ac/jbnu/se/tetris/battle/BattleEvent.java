package kr.ac.jbnu.se.tetris.battle;

import kr.ac.jbnu.se.tetris.core.GameEvent;

/** 전투 계층의 확정 사실과 원인 코어 이벤트를 담는 불변 기록 */
public final class BattleEvent {
    public enum Type {
        MATCH_STARTED, CORE_EVENT, DAMAGE, HP_CHANGED, GARBAGE_SENT,
        GARBAGE_RECEIVED, MATCH_FINISHED, PAUSED, RESUMED, ACTION_REJECTED,
        ITEM_ACQUIRED, ITEM_USED, ITEM_REMOVED
    }

    private final Type type;
    private final long eventId;
    private final String actorId;
    private final String targetId;
    private final int amount;
    private final String reason;
    private final GameEvent coreEvent;

    BattleEvent(Type type, long eventId, String actorId, String targetId, int amount,
                String reason, GameEvent coreEvent) {
        this.type = type;
        this.eventId = eventId;
        this.actorId = actorId;
        this.targetId = targetId;
        this.amount = amount;
        this.reason = reason;
        this.coreEvent = coreEvent;
    }

    public Type getType() { return type; }
    public long getEventId() { return eventId; }
    public String getActorId() { return actorId; }
    public String getTargetId() { return targetId; }
    public int getAmount() { return amount; }
    public String getReason() { return reason; }
    public GameEvent getCoreEvent() { return coreEvent; }
}
