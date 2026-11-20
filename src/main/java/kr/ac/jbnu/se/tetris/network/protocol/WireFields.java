package kr.ac.jbnu.se.tetris.network.protocol;

/** 통신 JSON 필드 이름의 단일 정의. 이름을 바꾸면 기존 클라이언트와 호환되지 않으므로 값은 고정한다. */
final class WireFields {
    // 공통 봉투
    static final String VERSION = "version";
    static final String MESSAGE = "message";
    static final String TYPE = "type";
    static final String KIND = "kind";
    static final String REQUEST_ID = "requestId";
    static final String REASON_CODE = "reasonCode";
    static final String ACCESS_TOKEN = "accessToken";

    // 방·경기 식별
    static final String ROOM = "room";
    static final String ROOM_ID = "roomId";
    static final String ROOM_VERSION = "roomVersion";
    static final String MATCH_ID = "matchId";
    static final String LOCAL_PARTICIPANT_ID = "localParticipantId";
    static final String MAX_PARTICIPANTS = "maxParticipants";
    static final String READY = "ready";
    static final String PHASE = "phase";

    // 갱신 본문
    static final String BATTLE = "battle";
    static final String EVENTS = "events";
    static final String OUTCOME = "outcome";
    static final String ACCEPTED = "accepted";
    static final String BATTLE_VERSION = "battleVersion";

    // 입력·아이템
    static final String INTENT = "intent";
    static final String ITEM_USE = "itemUse";
    static final String ITEM_ID = "itemId";
    static final String TARGET_ACTOR_ID = "targetActorId";
    static final String TARGET_CELL = "targetCell";
    static final String ACTOR_ID = "actorId";
    static final String EVENT_ID = "eventId";

    // 전투 사본
    static final String STATUS = "status";
    static final String REASON = "reason";
    static final String WINNER_ID = "winnerId";
    static final String ELAPSED_MILLIS = "elapsedMillis";
    static final String PARTICIPANTS = "participants";
    static final String MAX_HP = "maxHp";
    static final String ELIMINATED = "eliminated";
    static final String CHARACTER_ID = "characterId";
    static final String ITEM_SLOTS = "itemSlots";
    static final String ITEMS = "items";
    static final String ITEM_CHARGES = "itemCharges";
    static final String FEVER = "fever";
    static final String FEVER_REMAINING_MILLIS = "feverRemainingMillis";
    static final String TIME_WARP_REMAINING_MILLIS = "timeWarpRemainingMillis";
    static final String PENDING_GARBAGE_LINES = "pendingGarbageLines";
    static final String GARBAGE_WAIT_REMAINING_MILLIS = "garbageWaitRemainingMillis";
    static final String GRAVITY_MILLIS = "gravityMillis";
    static final String MAX_COMBO = "maxCombo";
    static final String TOTAL_DAMAGE = "totalDamage";

    // 게임 사본
    static final String BOARD = "board";
    static final String BOARD_ITEMS = "boardItems";
    static final String BOARD_ORIGINS = "boardOrigins";
    static final String ACTIVE_PIECE = "activePiece";
    static final String ROTATION = "rotation";
    static final String IDENTITY = "identity";
    static final String ORE_CELL_INDEX = "oreCellIndex";
    static final String PIECE_X = "pieceX";
    static final String PIECE_Y = "pieceY";
    static final String LINES_CLEARED = "linesCleared";
    static final String AWAITING_SPAWN = "awaitingSpawn";
    static final String HOLD_PIECE = "holdPiece";
    static final String HOLD_ITEM_ID = "holdItemId";
    static final String HOLD_ORE_CELL_INDEX = "holdOreCellIndex";
    static final String CAN_HOLD = "canHold";
    static final String NEXT_PIECES = "nextPieces";
    static final String GHOST_Y = "ghostY";
    static final String COMBO = "combo";

    private WireFields() { }
}
