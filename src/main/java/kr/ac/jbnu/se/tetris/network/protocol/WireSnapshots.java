package kr.ac.jbnu.se.tetris.network.protocol;

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
        value.put("status", battle.getStatus().name());
        value.put("version", battle.getVersion());
        if (battle.getWinnerId() != null) value.put("winnerId", battle.getWinnerId());
        if (battle.getReason() != null) value.put("reason", battle.getReason());
        List<Object> participants = new ArrayList<Object>();
        for (ParticipantState participant : battle.getParticipants().values()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("id", participant.getId());
            entry.put("name", participant.getName());
            entry.put("hp", participant.getHp());
            entry.put("maxHp", participant.getMaxHp());
            entry.put("eliminated", participant.isEliminated());
            entry.put("game", encodeGame(participant.getGameState()));
            participants.add(entry);
        }
        value.put("participants", participants);
        return value;
    }

    static BattleState decode(Object raw) throws IOException {
        Map<String, Object> value = WireCodec.object(raw);
        WireCodec.keys(value, new String[] { "status", "version", "participants" },
                new String[] { "winnerId", "reason" });
        BattleState.Status status = WireCodec.enumeration(BattleState.Status.class, value.get("status"));
        long version = WireCodec.nonnegative(value.get("version"));
        String winnerId = WireCodec.optionalId(value, "winnerId");
        String reason = WireCodec.optionalText(value, "reason", 256);
        List<Object> entries = WireCodec.array(value.get("participants"));
        if (entries.size() < 2 || entries.size() > 4) throw new IOException("Invalid participant count");
        Map<String, ParticipantState> participants = new LinkedHashMap<String, ParticipantState>();
        for (Object rawEntry : entries) {
            Map<String, Object> entry = WireCodec.object(rawEntry);
            WireCodec.keys(entry, new String[] { "id", "name", "hp", "maxHp", "eliminated", "game" },
                    new String[0]);
            String id = WireCodec.id(entry.get("id"));
            String name = WireCodec.text(entry.get("name"), 128);
            int hp = WireCodec.integer(entry.get("hp"));
            int maxHp = WireCodec.integer(entry.get("maxHp"));
            boolean eliminated = WireCodec.bool(entry.get("eliminated"));
            GameState game = decodeGame(entry.get("game"));
            if (participants.containsKey(id)) throw new IOException("Duplicate participant ID");
            participants.put(id, BattleSnapshots.participant(id, name, hp, maxHp, game, eliminated));
        }
        return BattleSnapshots.battle(status, version, participants, winnerId, reason);
    }

    private static Map<String, Object> encodeGame(GameState game) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("actorId", game.getActorId());
        value.put("version", game.getVersion());
        value.put("tick", game.getTick());
        value.put("status", game.getStatus().name());
        value.put("board", encodeBoard(game.getBoard()));
        if (game.getActivePiece() != null) {
            Map<String, Object> piece = new LinkedHashMap<String, Object>();
            piece.put("type", game.getActivePiece().getType().name());
            piece.put("rotation", game.getActivePiece().getRotation());
            value.put("activePiece", piece);
        }
        value.put("pieceX", game.getPieceX());
        value.put("pieceY", game.getPieceY());
        value.put("linesCleared", game.getLinesCleared());
        value.put("awaitingSpawn", game.isAwaitingSpawn());
        value.put("holdPiece", game.getHoldPiece().name());
        value.put("canHold", game.canHold());
        List<Object> next = new ArrayList<Object>();
        for (PieceType piece : game.getNextPieces()) next.add(piece.name());
        value.put("nextPieces", next);
        value.put("ghostY", game.getGhostY());
        value.put("combo", game.getCombo());
        value.put("pendingGarbageLines", game.getPendingGarbageLines());
        return value;
    }

    private static GameState decodeGame(Object raw) throws IOException {
        Map<String, Object> value = WireCodec.object(raw);
        WireCodec.keys(value, new String[] { "actorId", "version", "tick", "status", "board",
                "pieceX", "pieceY", "linesCleared", "awaitingSpawn", "holdPiece", "canHold",
                "nextPieces", "ghostY", "combo", "pendingGarbageLines" },
                new String[] { "activePiece" });
        String actorId = WireCodec.id(value.get("actorId"));
        long version = WireCodec.nonnegative(value.get("version"));
        long tick = WireCodec.nonnegative(value.get("tick"));
        GameState.Status status = WireCodec.enumeration(GameState.Status.class, value.get("status"));
        BoardState board = decodeBoard(value.get("board"));
        Piece active = null;
        if (value.containsKey("activePiece")) {
            Map<String, Object> piece = WireCodec.object(value.get("activePiece"));
            WireCodec.keys(piece, new String[] { "type", "rotation" }, new String[0]);
            PieceType type = WireCodec.enumeration(PieceType.class, piece.get("type"));
            int rotation = WireCodec.integer(piece.get("rotation"));
            if (rotation < 0 || rotation > 3) throw new IOException("Invalid piece rotation");
            active = new Piece(type);
            for (int index = 0; index < rotation; index++) active = active.rotateRight();
            if (active.getRotation() != rotation) throw new IOException("Invalid piece rotation");
        }
        List<Object> rawNext = WireCodec.array(value.get("nextPieces"));
        if (rawNext.size() > 3) throw new IOException("Too many next pieces");
        List<PieceType> next = new ArrayList<PieceType>();
        for (Object rawPiece : rawNext) next.add(WireCodec.enumeration(PieceType.class, rawPiece));
        return CoreSnapshots.game(actorId, version, tick, status, board, active,
                WireCodec.integer(value.get("pieceX")), WireCodec.integer(value.get("pieceY")),
                WireCodec.integer(value.get("linesCleared")), WireCodec.bool(value.get("awaitingSpawn")),
                WireCodec.enumeration(PieceType.class, value.get("holdPiece")),
                WireCodec.bool(value.get("canHold")), next, WireCodec.integer(value.get("ghostY")),
                WireCodec.integer(value.get("combo")), WireCodec.integer(value.get("pendingGarbageLines")));
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
