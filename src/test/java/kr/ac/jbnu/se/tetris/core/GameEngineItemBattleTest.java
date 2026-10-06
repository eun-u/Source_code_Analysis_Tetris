package kr.ac.jbnu.se.tetris.core;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/** 아이템 원본 식별과 전투 착지 경계를 실제 명령 순서로 검증한다. */
public final class GameEngineItemBattleTest {
    public static void main(String[] args) throws Exception {
        fifthPieceGetsItem();
        holdKeepsItemIdentity();
        clearsTwoOriginalPiecesOnceEach();
        managedLockAndGarbageBoundary();
    }

    private static void fifthPieceGetsItem() {
        GameEngine engine = engine();
        engine.setBattleManaged(true);
        engine.configureItemSpawns(5, 19L);
        send(engine, GameAction.Type.START, 0);
        long sequence = 1;
        for (int index = 1; index <= 4; index++) {
            check(engine.getState().getActivePiece().getItemId() == null,
                    "Only the fifth original piece receives an item");
            send(engine, GameAction.Type.HARD_DROP, sequence++);
            send(engine, GameAction.Type.GRAVITY_TICK, sequence++);
        }
        check(engine.getState().getActivePiece().getItemId() != null,
                "Fifth original piece has one item");
    }

    private static void holdKeepsItemIdentity() {
        GameEngine engine = engine();
        engine.configureItemSpawns(1, 21L);
        send(engine, GameAction.Type.START, 0);
        Piece first = engine.getState().getActivePiece();
        check(first.getItemId() != null, "Spawned item piece");
        send(engine, GameAction.Type.HOLD, 1);
        check(first.getItemId().equals(engine.getState().getHoldItemId()), "HOLD retains item");
        send(engine, GameAction.Type.HARD_DROP, 2);
        send(engine, GameAction.Type.HOLD, 3);
        Piece restored = engine.getState().getActivePiece();
        check(restored.getIdentity() == first.getIdentity()
                && first.getItemId().equals(restored.getItemId()), "Swap retains original identity");
    }

    private static void clearsTwoOriginalPiecesOnceEach() throws Exception {
        GameEngine engine = engine();
        engine.configureItemSpawns(1, 3L);
        send(engine, GameAction.Type.START, 0);
        Board board = boardOf(engine);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 10; x++) {
            if (x != 4 && x != 5 && x != 6 && x != 7) board.setCell(x, y, PieceType.T);
        }
        Piece first = engine.getState().getActivePiece();
        send(engine, GameAction.Type.HARD_DROP, 1);
        check(engine.getState().getBoard().getItemId(6, 0).equals(first.getItemId()),
                "Placed item metadata follows board cell");
        Piece second = engine.getState().getActivePiece();
        send(engine, GameAction.Type.MOVE_LEFT, 2);
        send(engine, GameAction.Type.MOVE_LEFT, 3);
        ActionResult clear = send(engine, GameAction.Type.HARD_DROP, 4);
        GameEvent event = event(clear.getEvents(), GameEvent.Type.LINE_CLEAR);
        check(event != null && event.getCollectedItems().size() == 2,
                "Both source pieces collect once when rows clear");
        check(event.getCollectedItems().contains(first.getItemId())
                && event.getCollectedItems().contains(second.getItemId()), "Correct item IDs collected");
        boolean immutable = false;
        try { event.getCollectedItems().clear(); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "Collected item list is immutable");
    }

    private static void managedLockAndGarbageBoundary() {
        GameEngine engine = engine();
        engine.setBattleManaged(true);
        send(engine, GameAction.Type.START, 0);
        ActionResult locked = send(engine, GameAction.Type.HARD_DROP, 1);
        check(locked.getState().isAwaitingSpawn() && locked.getState().getActivePiece() == null,
                "Managed lock waits for battle effects even without line clear");
        check(!send(engine, GameAction.Type.MOVE_LEFT, 2).isAccepted(), "Cannot move during boundary");
        ActionResult raised = engine.applyGarbage(Arrays.asList(new GameAction.Garbage(2, 3)));
        check(raised.isAccepted() && event(raised.getEvents(), GameEvent.Type.GARBAGE_RECEIVED) != null,
                "Managed garbage applied at boundary");
        check(raised.getState().getBoard().getCell(3, 0) == PieceType.EMPTY
                && raised.getState().getBoard().getCell(2, 0) == PieceType.GARBAGE,
                "Garbage geometry");
        ActionResult cleaned = engine.clearBottomGarbageLine();
        check(cleaned.isAccepted() && event(cleaned.getEvents(), GameEvent.Type.GARBAGE_CLEANED) != null,
                "Cleaner removes bottom garbage row");
        check(send(engine, GameAction.Type.GRAVITY_TICK, 3).getState().getActivePiece() != null,
                "Battle resumes spawning on explicit gravity tick");
    }

    private static GameEngine engine() {
        return new GameEngine(new PieceGenerator() {
            public PieceType nextPiece() { return PieceType.O; }
        });
    }

    private static Board boardOf(GameEngine engine) throws Exception {
        Field field = GameEngine.class.getDeclaredField("board");
        field.setAccessible(true);
        return (Board) field.get(engine);
    }

    private static ActionResult send(GameEngine engine, GameAction.Type type, long sequence) {
        return engine.dispatch(new GameAction(type, "local", sequence));
    }

    private static GameEvent event(List<GameEvent> events, GameEvent.Type type) {
        for (GameEvent event : events) if (event.getType() == type) return event;
        return null;
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
