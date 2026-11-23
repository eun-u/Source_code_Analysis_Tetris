package kr.ac.jbnu.se.tetris.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import kr.ac.jbnu.se.tetris.item.ItemIds;

/** 순서 있는 명령 처리와 UI용 스냅샷 조회를 제공하는 동기화된 순수 게임 규칙 엔진 */
public final class GameEngine implements GameActionSink {
    private static final int MAX_PENDING_GARBAGE_LINES = 220;
    private static final List<String> ITEM_IDS = ItemIds.ALL;
    private final String actorId;
    private final PieceGenerator generator;
    private final Board board = new Board();
    private final ArrayDeque<Piece> nextPieces = new ArrayDeque<Piece>();
    // 후속 보충 실패 시에도 미리 뽑은 블록 보존 및 공개 스냅샷 불변 유지
    private final ArrayDeque<PieceType> reservedPieces = new ArrayDeque<PieceType>();
    private final ArrayDeque<GameAction.Garbage> pendingGarbage = new ArrayDeque<GameAction.Garbage>();
    private final Set<Long> collectedItemOrigins = new HashSet<Long>();
    private GameState.Status status = GameState.Status.READY;
    private Piece activePiece;
    private PieceType holdPiece = PieceType.EMPTY;
    private Piece heldPiece;
    private int pieceX;
    private int pieceY;
    private int linesCleared;
    private int combo = -1;
    private int pendingGarbageLines;
    private boolean awaitingSpawn;
    private boolean holdUsed;
    private boolean lastSuccessfulRotation;
    private long version;
    private long tick;
    private long lastSequence = -1;
    private long lastEventId;
    private long issuedPieces;
    private boolean battleManaged;
    private int itemEveryPieces;
    private Random itemRandom;
    private List<String> itemPool = ITEM_IDS;

    public GameEngine(PieceGenerator generator) { this("local", generator); }

    public GameEngine(String actorId, PieceGenerator generator) {
        if (actorId == null || actorId.trim().isEmpty() || generator == null) {
            throw new IllegalArgumentException("Engine needs an actor ID and piece generator");
        }
        this.actorId = actorId;
        this.generator = generator;
    }

    /** 전투 규칙 계층이 공격·상쇄를 처리할 때 다음 블록 생성을 명시적으로 미룬다. */
    public synchronized void setBattleManaged(boolean enabled) {
        if (status != GameState.Status.READY) throw new IllegalStateException("Configure before START");
        battleManaged = enabled;
    }

    /** 지정한 수의 새 미노마다 한 번 아이템을 부여한다. */
    public synchronized void configureItemSpawns(int everyPieces, long seed) {
        configureItemSpawns(everyPieces, seed, ITEM_IDS);
    }

    /** PvE 몬스터 전용으로 출현 가능한 아이템 종류도 제한한다. */
    public synchronized void configureItemSpawns(int everyPieces, long seed,
                                                  List<String> allowedItems) {
        if (status != GameState.Status.READY) throw new IllegalStateException("Configure before START");
        if (everyPieces < 0 || allowedItems == null
                || (everyPieces > 0 && allowedItems.isEmpty())
                || !ITEM_IDS.containsAll(allowedItems)
                || new HashSet<String>(allowedItems).size() != allowedItems.size())
            throw new IllegalArgumentException("Invalid item spawn policy");
        itemEveryPieces = everyPieces;
        itemPool = Collections.unmodifiableList(new ArrayList<String>(allowedItems));
        itemRandom = everyPieces == 0 ? null : new Random(seed);
    }

    /** 내부 가변 보드와 분리된 현재 상태를 반환하며 고스트 착지 위치도 함께 계산 */
    public synchronized GameState getState() {
        int ghostY = activePiece == null ? -1 : board.landingY(activePiece, pieceX, pieceY);
        return new GameState(actorId, version, tick, status, board.snapshot(),
                activePiece, pieceX, pieceY, linesCleared, awaitingSpawn,
                holdPiece, status == GameState.Status.RUNNING && activePiece != null && !holdUsed,
                nextTypes(), ghostY, combo, pendingGarbageLines, getHoldItemId(),
                heldPiece == null ? -1 : heldPiece.getOreCellIndex());
    }

    public synchronized String getHoldItemId() { return heldPiece == null ? null : heldPiece.getItemId(); }

    private List<PieceType> nextTypes() {
        List<PieceType> types = new ArrayList<PieceType>(nextPieces.size());
        for (Piece piece : nextPieces) types.add(piece.getType());
        return types;
    }

    /** 행위자와 순번을 확인한 뒤 명령 하나의 상태 변경과 이벤트 생성을 완료 */
    public synchronized ActionResult dispatch(GameAction action) {
        if (action == null) return reject("Action is null");
        if (!actorId.equals(action.getActorId())) return reject("Wrong actor ID");
        if (action.getSequence() < 0 || action.getSequence() <= lastSequence) {
            return reject("Sequence must increase and be non-negative");
        }
        // 상태 전이 거절 시에도 새 입력 순번을 소모하여 재전송 우회 방지
        lastSequence = action.getSequence();
        GameAction.Type type = action.getType();
        if (type == null) return reject("Action type is null");
        if (type == GameAction.Type.USE_ITEM) return reject("USE_ITEM is not implemented in Phase 1");
        switch (type) {
            case START:
                return start();
            case PAUSE:
                return changeRunState(GameState.Status.RUNNING, GameState.Status.PAUSED,
                        GameEvent.Type.PAUSED, "PAUSE requires RUNNING");
            case RESUME:
                return changeRunState(GameState.Status.PAUSED, GameState.Status.RUNNING,
                        GameEvent.Type.RESUMED, "RESUME requires PAUSED");
            default:
                return dispatchWhileRunning(action, type);
        }
    }

    /** 실행 중에만 허용되는 명령을 종류별 처리 메서드로 전달한다. */
    private ActionResult dispatchWhileRunning(GameAction action, GameAction.Type type) {
        if (status != GameState.Status.RUNNING) return reject(type + " requires RUNNING");
        if (type == GameAction.Type.RECEIVE_GARBAGE) return receiveGarbage(action.getGarbage());
        // 줄 제거 뒤에는 다음 중력 틱에서 새 블록을 생성해 입력 순서를 일정하게 유지
        if (awaitingSpawn) return spawnAfterLineClear(type);
        if (activePiece == null) return reject("No active piece");
        switch (type) {
            case HOLD:
                return hold();
            case MOVE_LEFT:
            case MOVE_RIGHT:
                return move(type);
            case ROTATE_LEFT:
            case ROTATE_RIGHT:
                return rotate(type);
            case SOFT_DROP:
            case GRAVITY_TICK:
                return drop(type);
            case HARD_DROP:
                return hardDrop();
            default:
                return reject("Unsupported action: " + type);
        }
    }

    private ActionResult start() {
        if (status != GameState.Status.READY) return reject("START requires READY");
        PreparedPiece next = prepareNextPiece();
        if (next.reason != null) return reject(next.reason);
        Piece first = consumePreparedPiece();
        status = GameState.Status.RUNNING;
        version++;
        List<GameEvent> events = new ArrayList<GameEvent>();
        events.add(event(GameEvent.Type.GAME_STARTED, null, 0, 0, 0, null));
        spawn(first, true, events);
        return accepted(events);
    }

    /** PAUSE/RESUME처럼 실행 상태만 바꾸는 명령의 공통 처리 */
    private ActionResult changeRunState(GameState.Status required, GameState.Status next,
                                        GameEvent.Type eventType, String rejection) {
        if (status != required) return reject(rejection);
        status = next;
        version++;
        return acceptedWith(event(eventType, null, 0, 0, 0, null));
    }

    private ActionResult receiveGarbage(GameAction.Garbage payload) {
        if (payload == null) return reject("RECEIVE_GARBAGE requires a payload");
        if (pendingGarbageLines > MAX_PENDING_GARBAGE_LINES - payload.getLines()) {
            return reject("Too many pending garbage lines");
        }
        pendingGarbage.addLast(payload);
        pendingGarbageLines += payload.getLines();
        version++;
        return acceptedWith(event(GameEvent.Type.GARBAGE_QUEUED, null, 0, 0, payload.getLines(), null));
    }

    private ActionResult spawnAfterLineClear(GameAction.Type type) {
        if (type != GameAction.Type.GRAVITY_TICK) return reject("Waiting for next gravity tick to spawn");
        PreparedPiece next = prepareNextPiece();
        if (next.reason != null) return reject(next.reason);
        Piece piece = consumePreparedPiece();
        version++;
        tick++;
        awaitingSpawn = false;
        List<GameEvent> events = new ArrayList<GameEvent>();
        spawn(piece, true, events);
        return accepted(events);
    }

    // 현재 블록당 HOLD 한 번 허용 및 교체 블록 확보 후 상태 변경
    private ActionResult hold() {
        if (holdUsed) return reject("HOLD already used for this piece");
        Piece previous = activePiece;
        Piece replacement;
        if (heldPiece == null) {
            PreparedPiece next = prepareNextPiece();
            if (next.reason != null) return reject(next.reason);
            replacement = consumePreparedPiece();
        } else {
            replacement = copyOf(heldPiece);
        }
        heldPiece = copyOf(previous);
        holdPiece = previous.getType();
        holdUsed = true;
        lastSuccessfulRotation = false;
        version++;
        List<GameEvent> events = new ArrayList<GameEvent>();
        events.add(event(GameEvent.Type.PIECE_HELD, previous, pieceX, pieceY, 0, null));
        spawn(replacement, false, events);
        return accepted(events);
    }

    private static Piece copyOf(Piece piece) {
        return new Piece(piece.getType(), piece.getIdentity(), piece.getItemId(), piece.getOreCellIndex());
    }

    private ActionResult move(GameAction.Type type) {
        int nextX = pieceX + (type == GameAction.Type.MOVE_LEFT ? -1 : 1);
        if (!board.canPlace(activePiece, nextX, pieceY)) return reject("Piece collision");
        pieceX = nextX;
        lastSuccessfulRotation = false;
        version++;
        return acceptedWith(event(GameEvent.Type.PIECE_MOVED, activePiece, pieceX, pieceY, 0, null));
    }

    private ActionResult rotate(GameAction.Type type) {
        Piece rotated = type == GameAction.Type.ROTATE_LEFT
                ? activePiece.rotateLeft() : activePiece.rotateRight();
        if (!board.canPlace(rotated, pieceX, pieceY)) return reject("Piece collision");
        activePiece = rotated;
        lastSuccessfulRotation = activePiece.getType() == PieceType.T;
        version++;
        return acceptedWith(event(GameEvent.Type.PIECE_ROTATED, activePiece, pieceX, pieceY, 0, null));
    }

    /** SOFT_DROP과 GRAVITY_TICK: 한 칸 내리거나, 더 내릴 수 없으면 블록을 고정한다. */
    private ActionResult drop(GameAction.Type type) {
        boolean canMove = board.canPlace(activePiece, pieceX, pieceY - 1);
        LockPreview preview = null;
        PreparedPiece next = null;
        if (!canMove) {
            preview = previewLock(pieceY, lastSuccessfulRotation);
            next = prepareForLock(preview);
            if (next != null && next.reason != null) return reject(next.reason);
        }
        version++;
        if (type == GameAction.Type.GRAVITY_TICK) tick++;
        List<GameEvent> events = new ArrayList<GameEvent>();
        if (canMove) {
            pieceY--;
            lastSuccessfulRotation = false;
            events.add(event(GameEvent.Type.PIECE_MOVED, activePiece, pieceX, pieceY, 0, null));
        } else {
            lock(preview, next == null ? null : consumePreparedPiece(), events);
        }
        return accepted(events);
    }

    private ActionResult hardDrop() {
        int landingY = board.landingY(activePiece, pieceX, pieceY);
        LockPreview preview = previewLock(landingY, lastSuccessfulRotation && landingY == pieceY);
        PreparedPiece next = prepareForLock(preview);
        if (next != null && next.reason != null) return reject(next.reason);
        version++;
        List<GameEvent> events = new ArrayList<GameEvent>();
        pieceY = landingY;
        lock(preview, next == null ? null : consumePreparedPiece(), events);
        return accepted(events);
    }

    /** 줄이 지워지지 않는 일반 고정이면 다음 블록을 미리 준비하고, 그 외에는 null을 돌려준다. */
    private PreparedPiece prepareForLock(LockPreview preview) {
        if (battleManaged || preview.linesCleared != 0 || preview.overflow) return null;
        return prepareNextPiece();
    }

    private void spawn(Piece next, boolean resetHold, List<GameEvent> events) {
        BoardState snapshot = board.snapshot();
        int nextX = PlacementSimulator.spawnX(snapshot);
        int nextY = PlacementSimulator.spawnY(snapshot, next);
        lastSuccessfulRotation = false;
        if (resetHold) holdUsed = false;
        if (!board.canPlace(next, nextX, nextY)) {
            activePiece = null;
            status = GameState.Status.GAME_OVER;
            topOut(events, "TOP_OUT");
            return;
        }
        activePiece = next;
        pieceX = nextX;
        pieceY = nextY;
        events.add(event(GameEvent.Type.PIECE_SPAWNED, next, nextX, nextY, 0, null));
    }

    private LockPreview previewLock(int landingY, boolean rotatedAtLock) {
        Board detached = new Board(board.snapshot());
        detached.place(activePiece, pieceX, landingY);
        boolean tSpin = rotatedAtLock && activePiece.getType() == PieceType.T
                && occupiedCorners(detached, pieceX, landingY) >= 3;
        Map<Long, GameEvent.ItemExtraction> collected = detached.itemsOnCompletedRows(collectedItemOrigins);
        int cleared = detached.removeFullLines();
        detached.clearCollectedItemMarkers(collected.keySet());
        // 퍼펙트 클리어는 줄 제거 직후 가비지를 올리기 전에 보드가 완전히 비었는지로 판정
        boolean perfectClear = cleared > 0 && detached.isEmpty();
        boolean overflow = false;
        for (GameAction.Garbage garbage : battleManaged
                ? new ArrayList<GameAction.Garbage>() : pendingGarbage) {
            for (int i = 0; i < garbage.getLines() && !overflow; i++) {
                overflow |= detached.addGarbageLine(garbage.getHoleColumn());
            }
        }
        return new LockPreview(detached, cleared, tSpin, perfectClear, overflow, collected);
    }

    private static int occupiedCorners(Board board, int x, int y) {
        int occupied = 0;
        int[] offsets = {-1, 1};
        for (int dx : offsets) for (int dy : offsets) {
            int cx = x + dx;
            int cy = y + dy;
            if (cx < 0 || cx >= Board.WIDTH || cy < 0 || cy >= Board.HEIGHT
                    || board.getCell(cx, cy) != PieceType.EMPTY) occupied++;
        }
        return occupied;
    }

    /** 전투가 상쇄를 마친 가비지만 다음 생성 전에 즉시 분출한다. */
    public synchronized ActionResult applyGarbage(List<GameAction.Garbage> batches) {
        if (!battleManaged || status != GameState.Status.RUNNING || !awaitingSpawn) {
            return reject("Garbage requires a managed lock boundary");
        }
        if (batches == null) return reject("Garbage batches are null");
        int total = totalGarbageLines(batches);
        if (total < 0) return reject("Invalid garbage batch");
        if (total == 0) return accepted(new ArrayList<GameEvent>());
        Board detached = new Board(board.snapshot());
        boolean overflow = stackGarbage(detached, batches);
        board.copyFrom(detached);
        version++;
        List<GameEvent> events = new ArrayList<GameEvent>();
        events.add(event(GameEvent.Type.GARBAGE_RECEIVED, null, 0, 0, total, null));
        if (overflow) {
            awaitingSpawn = false;
            status = GameState.Status.GAME_OVER;
            topOut(events, "GARBAGE_TOP_OUT");
        }
        return accepted(events);
    }

    /** 가비지 묶음의 총 줄 수를 구한다. 묶음이 null이거나 한도를 넘으면 -1. */
    private static int totalGarbageLines(List<GameAction.Garbage> batches) {
        int total = 0;
        for (GameAction.Garbage batch : batches) {
            if (batch == null || total > MAX_PENDING_GARBAGE_LINES - batch.getLines()) return -1;
            total += batch.getLines();
        }
        return total;
    }

    /** 가비지를 보드에 쌓고, 한 줄이라도 넘쳤으면 true를 돌려준다. */
    private static boolean stackGarbage(Board target, List<GameAction.Garbage> batches) {
        boolean overflow = false;
        for (GameAction.Garbage batch : batches) {
            for (int i = 0; i < batch.getLines(); i++) {
                overflow |= target.addGarbageLine(batch.getHoleColumn());
            }
        }
        return overflow;
    }

    /** 아이템 효과가 실제로 제거할 가비지 행이 있을 때만 성공한다. */
    public synchronized ActionResult clearBottomGarbageLine() {
        if (status != GameState.Status.RUNNING) return reject("Cleaning requires RUNNING");
        Board detached = new Board(board.snapshot());
        if (!detached.clearBottomGarbageLine()) return reject("No garbage line to clean");
        if (activePiece != null && !detached.canPlace(activePiece, pieceX, pieceY)) {
            return reject("Cleaning would collide with active piece");
        }
        board.copyFrom(detached);
        version++;
        List<GameEvent> events = new ArrayList<GameEvent>();
        events.add(event(GameEvent.Type.GARBAGE_CLEANED, null, 0, 0, 1, null));
        return accepted(events);
    }

    private void lock(LockPreview preview, Piece next, List<GameEvent> events) {
        Piece placed = activePiece;
        int placedX = pieceX;
        int placedY = pieceY;
        board.copyFrom(preview.board);
        activePiece = null;
        lastSuccessfulRotation = false;
        events.add(event(GameEvent.Type.PIECE_PLACED, placed, placedX, placedY, 0, null));
        if (preview.tSpin) {
            events.add(event(GameEvent.Type.T_SPIN, placed, placedX, placedY,
                    preview.linesCleared, null, combo, true));
        }
        if (preview.linesCleared > 0) {
            recordLineClear(preview, events);
        } else {
            combo = -1;
        }
        flushPendingGarbage(events);
        if (preview.overflow) {
            awaitingSpawn = false;
            status = GameState.Status.GAME_OVER;
            topOut(events, "GARBAGE_TOP_OUT");
        } else if (battleManaged || preview.linesCleared > 0) {
            awaitingSpawn = true;
        } else {
            spawn(next, true, events);
        }
    }

    private void recordLineClear(LockPreview preview, List<GameEvent> events) {
        linesCleared += preview.linesCleared;
        combo++;
        collectedItemOrigins.addAll(preview.collected.keySet());
        events.add(event(GameEvent.Type.LINE_CLEAR, null, 0, 0,
                preview.linesCleared, null, combo, preview.tSpin, preview.perfectClear,
                itemIds(preview.collected), new ArrayList<GameEvent.ItemExtraction>(preview.collected.values())));
        if (combo > 0 && !preview.perfectClear) events.add(event(GameEvent.Type.COMBO, null, 0, 0,
                preview.linesCleared, null, combo, preview.tSpin));
        // 퍼펙트 클리어는 콤보를 이어가지 않으므로 다음 줄 제거가 0콤보가 되도록 콤보 없음 상태로 되돌림
        if (preview.perfectClear) combo = -1;
    }

    private void flushPendingGarbage(List<GameEvent> events) {
        if (battleManaged || pendingGarbageLines <= 0) return;
        events.add(event(GameEvent.Type.GARBAGE_RECEIVED, null, 0, 0, pendingGarbageLines, null));
        pendingGarbage.clear();
        pendingGarbageLines = 0;
    }

    private void topOut(List<GameEvent> events, String reason) {
        events.add(event(GameEvent.Type.TOP_OUT, null, 0, 0, 0, reason));
        events.add(event(GameEvent.Type.GAME_OVER, null, 0, 0, 0, reason));
    }

    /** 공개 상태를 바꾸기 전에 다음 블록과 미리보기 세 개를 모두 준비 */
    private PreparedPiece prepareNextPiece() {
        int needed = nextPieces.isEmpty() ? 4 : 1;
        while (reservedPieces.size() < needed) {
            final PieceType type;
            try {
                type = generator.nextPiece();
            } catch (RuntimeException failure) {
                String detail = failure.getMessage();
                return new PreparedPiece("Piece generator failed: "
                        + failure.getClass().getSimpleName() + (detail == null ? "" : ": " + detail));
            }
            if (type == null || type == PieceType.EMPTY || type == PieceType.GARBAGE) {
                return new PreparedPiece("Piece generator returned " + type);
            }
            reservedPieces.addLast(type);
        }
        return new PreparedPiece(null);
    }

    private Piece consumePreparedPiece() {
        Piece piece;
        if (nextPieces.isEmpty()) {
            piece = issuePiece(reservedPieces.removeFirst());
            for (int i = 0; i < 3; i++) nextPieces.addLast(issuePiece(reservedPieces.removeFirst()));
        } else {
            piece = nextPieces.removeFirst();
            nextPieces.addLast(issuePiece(reservedPieces.removeFirst()));
        }
        return piece;
    }

    private Piece issuePiece(PieceType type) {
        long origin = ++issuedPieces;
        String item = itemEveryPieces > 0 && origin % itemEveryPieces == 0
                ? itemPool.get(itemRandom.nextInt(itemPool.size())) : null;
        return new Piece(type, origin, item, item == null ? -1 : itemRandom.nextInt(4));
    }

    private static List<String> itemIds(Map<Long, GameEvent.ItemExtraction> collected) {
        List<String> ids = new ArrayList<String>();
        for (GameEvent.ItemExtraction extraction : collected.values()) ids.add(extraction.getItemId());
        return ids;
    }

    private static final class LockPreview {
        private final Board board;
        private final int linesCleared;
        private final boolean tSpin;
        private final boolean perfectClear;
        private final boolean overflow;
        private final Map<Long, GameEvent.ItemExtraction> collected;

        private LockPreview(Board board, int linesCleared, boolean tSpin, boolean perfectClear,
                            boolean overflow, Map<Long, GameEvent.ItemExtraction> collected) {
            this.board = board;
            this.linesCleared = linesCleared;
            this.tSpin = tSpin;
            this.perfectClear = perfectClear;
            this.overflow = overflow;
            this.collected = collected;
        }
    }

    private static final class PreparedPiece {
        private final String reason;
        private PreparedPiece(String reason) { this.reason = reason; }
    }

    private GameEvent event(GameEvent.Type type, Piece piece, int x, int y, int lineCount, String reason) {
        return event(type, piece, x, y, lineCount, reason, -1, false);
    }

    private GameEvent event(GameEvent.Type type, Piece piece, int x, int y, int lineCount,
                            String reason, int eventCombo, boolean tSpin) {
        return event(type, piece, x, y, lineCount, reason, eventCombo, tSpin, false);
    }
    private GameEvent event(GameEvent.Type type, Piece piece, int x, int y, int lineCount,
                            String reason, int eventCombo, boolean tSpin, boolean perfectClear) {
        return event(type, piece, x, y, lineCount, reason, eventCombo, tSpin, perfectClear,
                new ArrayList<String>());
    }

    private GameEvent event(GameEvent.Type type, Piece piece, int x, int y, int lineCount,
                            String reason, int eventCombo, boolean tSpin, boolean perfectClear,
                            List<String> collectedItems) {
        return event(type, piece, x, y, lineCount, reason, eventCombo, tSpin, perfectClear,
                collectedItems, Collections.<GameEvent.ItemExtraction>emptyList());
    }

    private GameEvent event(GameEvent.Type type, Piece piece, int x, int y, int lineCount,
                            String reason, int eventCombo, boolean tSpin, boolean perfectClear,
                            List<String> collectedItems, List<GameEvent.ItemExtraction> extractions) {
        return new GameEvent(type, ++lastEventId, version, tick, actorId,
                piece, x, y, lineCount, reason, eventCombo, tSpin, perfectClear, collectedItems, extractions);
    }

    private ActionResult accepted(List<GameEvent> events) {
        return new ActionResult(true, null, getState(), events);
    }

    private ActionResult acceptedWith(GameEvent event) {
        List<GameEvent> events = new ArrayList<GameEvent>();
        events.add(event);
        return accepted(events);
    }

    private ActionResult reject(String reason) {
        List<GameEvent> events = new ArrayList<GameEvent>();
        events.add(event(GameEvent.Type.ACTION_REJECTED, null, 0, 0, 0, reason));
        return new ActionResult(false, reason, getState(), events);
    }
}
