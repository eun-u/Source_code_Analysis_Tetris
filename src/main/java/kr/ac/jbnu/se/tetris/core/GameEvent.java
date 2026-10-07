package kr.ac.jbnu.se.tetris.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 게임에서 확정된 사실을 담는 불변 이벤트와 종류별 선택 필드 */
public final class GameEvent {
    public enum Type {
        GAME_STARTED, PIECE_SPAWNED, PIECE_MOVED, PIECE_ROTATED, PIECE_PLACED,
        LINE_CLEAR, COMBO, T_SPIN, PIECE_HELD, GARBAGE_QUEUED, GARBAGE_RECEIVED,
        GARBAGE_CLEANED, PAUSED, RESUMED, TOP_OUT, GAME_OVER, ACTION_REJECTED
    }

    private final Type type;
    private final long eventId;
    private final long stateVersion;
    private final long tick;
    private final String actorId;
    private final Piece piece;
    private final int x;
    private final int y;
    private final int lineCount;
    private final String reason;
    private final int combo;
    private final boolean tSpin;
    private final boolean perfectClear;
    private final List<String> collectedItems;

    GameEvent(Type type, long eventId, long stateVersion, long tick, String actorId,
              Piece piece, int x, int y, int lineCount, String reason) {
        this(type, eventId, stateVersion, tick, actorId, piece, x, y, lineCount, reason, -1, false);
    }

    GameEvent(Type type, long eventId, long stateVersion, long tick, String actorId,
              Piece piece, int x, int y, int lineCount, String reason, int combo, boolean tSpin) {
        this(type, eventId, stateVersion, tick, actorId, piece, x, y, lineCount, reason, combo, tSpin, false);
    }

    GameEvent(Type type, long eventId, long stateVersion, long tick, String actorId,
              Piece piece, int x, int y, int lineCount, String reason, int combo, boolean tSpin,
              boolean perfectClear) {
        this(type, eventId, stateVersion, tick, actorId, piece, x, y, lineCount,
                reason, combo, tSpin, perfectClear, Collections.<String>emptyList());
    }

    GameEvent(Type type, long eventId, long stateVersion, long tick, String actorId,
              Piece piece, int x, int y, int lineCount, String reason, int combo, boolean tSpin,
              boolean perfectClear, List<String> collectedItems) {
        if (type == null || actorId == null || eventId <= 0 || stateVersion < 0 || tick < 0) {
            throw new IllegalArgumentException("Invalid event identity");
        }
        switch (type) {
            case PIECE_SPAWNED:
            case PIECE_MOVED:
            case PIECE_ROTATED:
            case PIECE_PLACED:
                if (piece == null) throw new IllegalArgumentException(type + " requires a piece");
                break;
            case LINE_CLEAR:
                if (lineCount <= 0) throw new IllegalArgumentException("LINE_CLEAR requires a line count");
                break;
            case GARBAGE_QUEUED:
            case GARBAGE_RECEIVED:
                if (lineCount <= 0) throw new IllegalArgumentException(type + " requires a line count");
                break;
            case TOP_OUT:
            case GAME_OVER:
            case ACTION_REJECTED:
                if (reason == null || reason.isEmpty()) throw new IllegalArgumentException(type + " requires a reason");
                break;
            default:
                break;
        }
        if (perfectClear && type != Type.LINE_CLEAR) {
            throw new IllegalArgumentException("Only LINE_CLEAR can be a perfect clear");
        }
        if (collectedItems == null || (type != Type.LINE_CLEAR && !collectedItems.isEmpty())) {
            throw new IllegalArgumentException("Only LINE_CLEAR can collect items");
        }
        this.type = type;
        this.eventId = eventId;
        this.stateVersion = stateVersion;
        this.tick = tick;
        this.actorId = actorId;
        this.piece = piece;
        this.x = x;
        this.y = y;
        this.lineCount = lineCount;
        this.reason = reason;
        this.combo = combo;
        this.tSpin = tSpin;
        this.perfectClear = perfectClear;
        this.collectedItems = Collections.unmodifiableList(new ArrayList<String>(collectedItems));
    }

    public Type getType() { return type; }
    public long getEventId() { return eventId; }
    public long getStateVersion() { return stateVersion; }
    public long getTick() { return tick; }
    public String getActorId() { return actorId; }
    public Piece getPiece() { return piece; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getLineCount() { return lineCount; }
    public String getReason() { return reason; }
    public int getCombo() { return combo; }
    public boolean isTSpin() { return tSpin; }
    /** 줄 제거 직후 가비지 삽입 전 보드가 완전히 비었는지 여부, LINE_CLEAR에서만 true */
    public boolean isPerfectClear() { return perfectClear; }
    public List<String> getCollectedItems() { return collectedItems; }
}
