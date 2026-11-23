package kr.ac.jbnu.se.tetris.network.protocol;

import static kr.ac.jbnu.se.tetris.network.protocol.WireFields.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.jbnu.se.tetris.battle.BattleSnapshots;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.BoardState;
import kr.ac.jbnu.se.tetris.core.CoreSnapshots;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.Piece;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 검증된 전투 사본과 JSON 값의 양방향 변환 */
final class WireSnapshots {
    private WireSnapshots() { }

    static Map<String, Object> encode(BattleState battle) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(STATUS, battle.getStatus().name());
        value.put(VERSION, battle.getVersion());
        value.put(ELAPSED_MILLIS, battle.getElapsedMillis());
        if (battle.getWinnerId() != null) value.put(WINNER_ID, battle.getWinnerId());
        if (battle.getReason() != null) value.put(REASON, battle.getReason());
        List<Object> participants = new ArrayList<Object>();
        for (ParticipantState participant : battle.getParticipants().values()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("id", participant.getId());
            entry.put("name", participant.getName());
            entry.put("hp", participant.getHp());
            entry.put(MAX_HP, participant.getMaxHp());
            entry.put(ELIMINATED, participant.isEliminated());
            entry.put(CHARACTER_ID, participant.getCharacterId());
            entry.put(ITEM_SLOTS, participant.getItemSlots());
            entry.put(ITEMS, new ArrayList<String>(participant.getItems()));
            // 기존 온라인 클라이언트는 알 수 없는 필드를 거부하므로, 기본 1회 아이템만
            // 있는 공식 PvP 스냅샷에는 확장 필드를 보내지 않는다.
            boolean hasMultipleCharges = false;
            for (Integer charge : participant.getItemCharges())
                if (charge != null && charge.intValue() != 1) hasMultipleCharges = true;
            if (hasMultipleCharges)
                entry.put(ITEM_CHARGES, new ArrayList<Integer>(participant.getItemCharges()));
            entry.put(FEVER, participant.getFever());
            entry.put(FEVER_REMAINING_MILLIS, participant.getFeverRemainingMillis());
            entry.put(TIME_WARP_REMAINING_MILLIS, participant.getTimeWarpRemainingMillis());
            entry.put(PENDING_GARBAGE_LINES, participant.getPendingGarbageLines());
            entry.put(GARBAGE_WAIT_REMAINING_MILLIS, participant.getGarbageWaitRemainingMillis());
            entry.put(GRAVITY_MILLIS, participant.getGravityMillis());
            entry.put(MAX_COMBO, participant.getMaxCombo());
            entry.put(TOTAL_DAMAGE, participant.getTotalDamage());
            entry.put("game", encodeGame(participant.getGameState()));
            participants.add(entry);
        }
        value.put(PARTICIPANTS, participants);
        return value;
    }

    static BattleState decode(Object raw) throws IOException {
        Map<String, Object> value = WireCodec.object(raw);
        WireCodec.keys(value, new String[] { STATUS, VERSION, PARTICIPANTS },
                new String[] { WINNER_ID, REASON, ELAPSED_MILLIS });
        BattleState.Status status = WireCodec.enumeration(BattleState.Status.class, value.get(STATUS));
        long version = WireCodec.nonnegative(value.get(VERSION));
        String winnerId = WireCodec.optionalId(value, WINNER_ID);
        String reason = WireCodec.optionalText(value, REASON, 256);
        List<Object> entries = WireCodec.array(value.get(PARTICIPANTS));
        if (entries.size() < 2 || entries.size() > 4) throw new IOException("Invalid participant count");
        Map<String, ParticipantState> participants = new LinkedHashMap<String, ParticipantState>();
        for (Object rawEntry : entries) {
            ParticipantState participant = decodeParticipant(WireCodec.object(rawEntry), participants);
            participants.put(participant.getId(), participant);
        }
        return BattleSnapshots.battle(status, version, participants, winnerId, reason,
                optionalLong(value, ELAPSED_MILLIS));
    }

    /** 참가자 한 명을 검증한다. 중복 ID는 게임 사본 검증 직후 기존과 같은 순서로 거부한다. */
    private static ParticipantState decodeParticipant(Map<String, Object> entry,
            Map<String, ParticipantState> decoded) throws IOException {
        WireCodec.keys(entry, new String[] { "id", "name", "hp", MAX_HP, ELIMINATED, "game" },
                new String[] { CHARACTER_ID, ITEM_SLOTS, ITEMS, ITEM_CHARGES, FEVER, FEVER_REMAINING_MILLIS,
                        TIME_WARP_REMAINING_MILLIS, PENDING_GARBAGE_LINES, GARBAGE_WAIT_REMAINING_MILLIS,
                        GRAVITY_MILLIS, MAX_COMBO, TOTAL_DAMAGE });
        String id = WireCodec.id(entry.get("id"));
        String name = WireCodec.text(entry.get("name"), 128);
        int hp = WireCodec.integer(entry.get("hp"));
        int maxHp = WireCodec.integer(entry.get(MAX_HP));
        boolean eliminated = WireCodec.bool(entry.get(ELIMINATED));
        GameState game = decodeGame(entry.get("game"));
        if (decoded.containsKey(id)) throw new IOException("Duplicate participant ID");
        List<String> items = decodeInventory(entry);
        List<Integer> itemCharges = decodeItemCharges(entry, items.size());
        String characterId = entry.containsKey(CHARACTER_ID) ? WireCodec.id(entry.get(CHARACTER_ID)) : "student";
        return BattleSnapshots.participant(id, name, hp, maxHp, game, eliminated,
                characterId, optionalInt(entry, ITEM_SLOTS, 3), items, itemCharges, optionalInt(entry, FEVER, 0),
                optionalLong(entry, FEVER_REMAINING_MILLIS), optionalLong(entry, TIME_WARP_REMAINING_MILLIS),
                optionalInt(entry, PENDING_GARBAGE_LINES, game.getPendingGarbageLines()),
                optionalLong(entry, GARBAGE_WAIT_REMAINING_MILLIS), optionalInt(entry, GRAVITY_MILLIS, 400),
                optionalInt(entry, MAX_COMBO, 0), optionalInt(entry, TOTAL_DAMAGE, 0));
    }

    private static List<String> decodeInventory(Map<String, Object> entry) throws IOException {
        List<String> items = new ArrayList<String>();
        if (!entry.containsKey(ITEMS)) return items;
        List<Object> inventory = WireCodec.array(entry.get(ITEMS));
        if (inventory.size() > 4) throw new IOException("Too many inventory items");
        for (Object item : inventory) items.add(itemId(item));
        return items;
    }

    /** 확장 필드가 없던 기존 사본은 모든 아이템을 1회 사용으로 본다. */
    private static List<Integer> decodeItemCharges(Map<String, Object> entry, int itemCount) throws IOException {
        List<Integer> itemCharges = new ArrayList<Integer>();
        if (!entry.containsKey(ITEM_CHARGES)) {
            for (int i = 0; i < itemCount; i++) itemCharges.add(1);
            return itemCharges;
        }
        List<Object> charges = WireCodec.array(entry.get(ITEM_CHARGES));
        if (charges.size() != itemCount) throw new IOException("Item charge count mismatch");
        for (Object charge : charges) {
            int amount = WireCodec.integer(charge);
            if (amount < 1 || amount > 2) throw new IOException("Invalid item charge");
            itemCharges.add(amount);
        }
        return itemCharges;
    }

    private static Map<String, Object> encodeGame(GameState game) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(ACTOR_ID, game.getActorId());
        value.put(VERSION, game.getVersion());
        value.put("tick", game.getTick());
        value.put(STATUS, game.getStatus().name());
        value.put(BOARD, encodeBoard(game.getBoard()));
        List<Object> boardItems = new ArrayList<Object>();
        List<Object> boardOrigins = new ArrayList<Object>();
        for (int y = 0; y < 22; y++) for (int x = 0; x < 10; x++) {
            boardItems.add(game.getBoard().getItemId(x, y));
            boardOrigins.add(game.getBoard().getOriginId(x, y));
        }
        value.put(BOARD_ITEMS, boardItems);
        value.put(BOARD_ORIGINS, boardOrigins);
        if (game.getActivePiece() != null) {
            Map<String, Object> piece = new LinkedHashMap<String, Object>();
            piece.put(TYPE, game.getActivePiece().getType().name());
            piece.put(ROTATION, game.getActivePiece().getRotation());
            if (game.getActivePiece().getItemId() != null) {
                piece.put(ITEM_ID, game.getActivePiece().getItemId());
                piece.put(IDENTITY, game.getActivePiece().getIdentity());
                piece.put(ORE_CELL_INDEX, game.getActivePiece().getOreCellIndex());
            }
            value.put(ACTIVE_PIECE, piece);
        }
        value.put(PIECE_X, game.getPieceX());
        value.put(PIECE_Y, game.getPieceY());
        value.put(LINES_CLEARED, game.getLinesCleared());
        value.put(AWAITING_SPAWN, game.isAwaitingSpawn());
        value.put(HOLD_PIECE, game.getHoldPiece().name());
        if (game.getHoldItemId() != null) {
            value.put(HOLD_ITEM_ID, game.getHoldItemId());
            value.put(HOLD_ORE_CELL_INDEX, game.getHoldOreCellIndex());
        }
        value.put(CAN_HOLD, game.canHold());
        List<Object> next = new ArrayList<Object>();
        for (PieceType piece : game.getNextPieces()) next.add(piece.name());
        value.put(NEXT_PIECES, next);
        value.put(GHOST_Y, game.getGhostY());
        value.put(COMBO, game.getCombo());
        value.put(PENDING_GARBAGE_LINES, game.getPendingGarbageLines());
        return value;
    }

    private static GameState decodeGame(Object raw) throws IOException {
        Map<String, Object> value = WireCodec.object(raw);
        WireCodec.keys(value, new String[] { ACTOR_ID, VERSION, "tick", STATUS, BOARD,
                PIECE_X, PIECE_Y, LINES_CLEARED, AWAITING_SPAWN, HOLD_PIECE, CAN_HOLD,
                NEXT_PIECES, GHOST_Y, COMBO, PENDING_GARBAGE_LINES },
                new String[] { ACTIVE_PIECE, BOARD_ITEMS, BOARD_ORIGINS, HOLD_ITEM_ID, HOLD_ORE_CELL_INDEX });
        String actorId = WireCodec.id(value.get(ACTOR_ID));
        long version = WireCodec.nonnegative(value.get(VERSION));
        long tick = WireCodec.nonnegative(value.get("tick"));
        GameState.Status status = WireCodec.enumeration(GameState.Status.class, value.get(STATUS));
        BoardState board = decodeBoard(value.get(BOARD));
        if (value.containsKey(BOARD_ITEMS)) board = withBoardMetadata(board, value);
        Piece active = value.containsKey(ACTIVE_PIECE) ? decodeActivePiece(value.get(ACTIVE_PIECE)) : null;
        List<Object> rawNext = WireCodec.array(value.get(NEXT_PIECES));
        if (rawNext.size() > 3) throw new IOException("Too many next pieces");
        List<PieceType> next = new ArrayList<PieceType>();
        for (Object rawPiece : rawNext) next.add(WireCodec.enumeration(PieceType.class, rawPiece));
        return CoreSnapshots.game(actorId, version, tick, status, board, active,
                WireCodec.integer(value.get(PIECE_X)), WireCodec.integer(value.get(PIECE_Y)),
                WireCodec.integer(value.get(LINES_CLEARED)), WireCodec.bool(value.get(AWAITING_SPAWN)),
                WireCodec.enumeration(PieceType.class, value.get(HOLD_PIECE)),
                WireCodec.bool(value.get(CAN_HOLD)), next, WireCodec.integer(value.get(GHOST_Y)),
                WireCodec.integer(value.get(COMBO)), WireCodec.integer(value.get(PENDING_GARBAGE_LINES)),
                value.containsKey(HOLD_ITEM_ID) ? itemId(value.get(HOLD_ITEM_ID)) : null,
                optionalInt(value, HOLD_ORE_CELL_INDEX, value.containsKey(HOLD_ITEM_ID) ? 0 : -1));
    }

    /** 보드 칸별 아이템·출처 정보를 기존 보드 위에 덧입힌다. */
    private static BoardState withBoardMetadata(BoardState board, Map<String, Object> value) throws IOException {
        List<Object> metadata = WireCodec.array(value.get(BOARD_ITEMS));
        if (metadata.size() != 220) throw new IOException("Board item metadata requires 220 cells");
        String[] itemIds = new String[220];
        long[] origins = new long[220];
        List<Object> rawOrigins = value.containsKey(BOARD_ORIGINS)
                ? WireCodec.array(value.get(BOARD_ORIGINS)) : null;
        if (rawOrigins != null && rawOrigins.size() != 220) throw new IOException("Invalid board provenance");
        PieceType[] cells = new PieceType[220];
        for (int index = 0; index < 220; index++) {
            cells[index] = board.getCell(index % 10, index / 10);
            if (metadata.get(index) != null) itemIds[index] = itemId(metadata.get(index));
            if (rawOrigins != null) origins[index] = WireCodec.nonnegative(rawOrigins.get(index));
        }
        return CoreSnapshots.board(10, 22, cells, origins, itemIds);
    }

    private static Piece decodeActivePiece(Object raw) throws IOException {
        Map<String, Object> piece = WireCodec.object(raw);
        WireCodec.keys(piece, new String[] { TYPE, ROTATION },
                new String[] { ITEM_ID, IDENTITY, ORE_CELL_INDEX });
        PieceType type = WireCodec.enumeration(PieceType.class, piece.get(TYPE));
        int rotation = WireCodec.integer(piece.get(ROTATION));
        if (rotation < 0 || rotation > 3) throw new IOException("Invalid piece rotation");
        Piece active = new Piece(type);
        if (piece.containsKey(ITEM_ID)) {
            String itemId = itemId(piece.get(ITEM_ID));
            long identity = piece.containsKey(IDENTITY) ? WireCodec.positive(piece.get(IDENTITY)) : 1L;
            active = Piece.withItem(type, itemId, identity,
                    optionalInt(piece, ORE_CELL_INDEX, 0));
        } else if (piece.containsKey(ORE_CELL_INDEX)) {
            throw new IOException("Ore index without item");
        }
        for (int index = 0; index < rotation; index++) active = active.rotateRight();
        if (active.getRotation() != rotation) throw new IOException("Invalid piece rotation");
        return active;
    }

    private static int optionalInt(Map<String, Object> value, String key, int fallback) throws IOException {
        return value.containsKey(key) ? WireCodec.integer(value.get(key)) : fallback;
    }

    private static long optionalLong(Map<String, Object> value, String key) throws IOException {
        return value.containsKey(key) ? WireCodec.nonnegative(value.get(key)) : 0L;
    }

    private static String itemId(Object value) throws IOException {
        String id = WireCodec.id(value);
        if (!id.equals("damage_boost") && !id.equals("garbage_bomb") && !id.equals("heal")
                && !id.equals("shield") && !id.equals("line_cleaner") && !id.equals("fever_charge")
                && !id.equals("time_warp") && !id.equals("nullify")) {
            throw new IOException("Unknown item identifier");
        }
        return id;
    }

    private static String encodeBoard(BoardState board) {
        StringBuilder cells = new StringBuilder(220);
        for (int y = 0; y < 22; y++) {
            for (int x = 0; x < 10; x++) {
                PieceType cell = board.getCell(x, y);
                cells.append(cell == PieceType.EMPTY ? '.' : cell == PieceType.GARBAGE ? '#'
                        : cell.name().charAt(0));
            }
        }
        return cells.toString();
    }

    private static BoardState decodeBoard(Object raw) throws IOException {
        String cells = WireCodec.text(raw, 220);
        if (cells.length() != 220) throw new IOException("Board requires 220 cells");
        PieceType[] result = new PieceType[220];
        for (int index = 0; index < cells.length(); index++) {
            char cell = cells.charAt(index);
            if (cell == '.') result[index] = PieceType.EMPTY;
            else if (cell == '#') result[index] = PieceType.GARBAGE;
            else {
                try { result[index] = PieceType.valueOf(String.valueOf(cell)); }
                catch (IllegalArgumentException error) { throw new IOException("Invalid board cell", error); }
                if (result[index] == PieceType.EMPTY || result[index] == PieceType.GARBAGE) {
                    throw new IOException("Invalid board cell");
                }
            }
        }
        return CoreSnapshots.board(10, 22, result);
    }
}
