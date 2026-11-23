package kr.ac.jbnu.se.tetris.network.protocol;

import static kr.ac.jbnu.se.tetris.network.protocol.WireFields.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;
import kr.ac.jbnu.se.tetris.network.RequestOutcome;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;

/** 4바이트 길이와 UTF-8 JSON 버전 1의 엄격한 통신 경계 */
public final class WireCodec {
    public static final int MAX_FRAME_BYTES = 1024 * 1024;
    private static final int PROTOCOL_VERSION = 1;

    private WireCodec() { }

    /** WebSocket text message payload; TCP's four-byte length belongs only to TCP framing. */
    public static byte[] encodeRequestPayload(WireRequest request) throws IOException {
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        writeRequest(framed, request);
        byte[] bytes = framed.toByteArray();
        return java.util.Arrays.copyOfRange(bytes, 4, bytes.length);
    }

    public static WireRequest decodeRequestPayload(byte[] payload) throws IOException {
        return readRequest(new ByteArrayInputStream(framePayload(payload)));
    }

    public static byte[] encodeUpdatePayload(NetworkUpdate update) throws IOException {
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        writeUpdate(framed, update);
        byte[] bytes = framed.toByteArray();
        return java.util.Arrays.copyOfRange(bytes, 4, bytes.length);
    }

    public static NetworkUpdate decodeUpdatePayload(byte[] payload) throws IOException {
        return readUpdate(new ByteArrayInputStream(framePayload(payload)));
    }

    private static byte[] framePayload(byte[] payload) throws IOException {
        if (payload == null || payload.length < 1 || payload.length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid WebSocket message size");
        }
        byte[] framed = new byte[payload.length + 4];
        int length = payload.length;
        framed[0] = (byte) (length >>> 24);
        framed[1] = (byte) (length >>> 16);
        framed[2] = (byte) (length >>> 8);
        framed[3] = (byte) length;
        System.arraycopy(payload, 0, framed, 4, length);
        return framed;
    }

    public static void writeRequest(OutputStream output, WireRequest request) throws IOException {
        if (request == null) throw new IOException("Wire request is required");
        Map<String, Object> value = base("request");
        value.put(REQUEST_ID, request.getRequestId());
        value.put(KIND, request.getKind().name());
        switch (request.getKind()) {
            case ROOM:
                value.put(ROOM, encodeRoomCommand(request.getRoomCommand()));
                break;
            case INTENT:
                value.put(MATCH_ID, id(request.getMatchId()));
                value.put(INTENT, encodeIntent(request.getIntent()));
                break;
            case SNAPSHOT:
                value.put(MATCH_ID, id(request.getMatchId()));
                break;
            case AUTH_REFRESH:
                value.put(ACCESS_TOKEN, text(request.getAccessToken(), 8192));
                break;
            default:
                throw new IOException("Unknown request kind");
        }
        writeFrame(output, value);
    }

    public static WireRequest readRequest(InputStream input) throws IOException {
        Object raw = readFrame(input);
        if (raw == null) return null;
        try {
            Map<String, Object> value = object(raw);
            envelope(value, "request");
            WireRequest.Kind kind = enumeration(WireRequest.Kind.class, value.get(KIND));
            long requestId = positive(value.get(REQUEST_ID));
            switch (kind) {
                case ROOM:
                    keys(value, new String[] { VERSION, MESSAGE, REQUEST_ID, KIND, ROOM },
                            new String[0]);
                    return WireRequest.room(requestId, decodeRoomCommand(value.get(ROOM)));
                case INTENT:
                    keys(value, new String[] { VERSION, MESSAGE, REQUEST_ID, KIND, MATCH_ID, INTENT },
                            new String[0]);
                    return WireRequest.intent(requestId, id(value.get(MATCH_ID)), decodeIntent(value.get(INTENT)));
                case SNAPSHOT:
                    keys(value, new String[] { VERSION, MESSAGE, REQUEST_ID, KIND, MATCH_ID },
                            new String[0]);
                    return WireRequest.snapshot(requestId, id(value.get(MATCH_ID)));
                case AUTH_REFRESH:
                    keys(value, new String[] { VERSION, MESSAGE, REQUEST_ID, KIND, ACCESS_TOKEN },
                            new String[0]);
                    return WireRequest.authRefresh(requestId, text(value.get(ACCESS_TOKEN), 8192));
                default:
                    throw new IOException("Unknown request kind");
            }
        } catch (RuntimeException error) {
            throw new IOException("Invalid wire request", error);
        }
    }

    public static void writeUpdate(OutputStream output, NetworkUpdate update) throws IOException {
        if (update == null) throw new IOException("Network update is required");
        Map<String, Object> value = base("update");
        value.put(TYPE, update.getType().name());
        switch (update.getType()) {
            case CONNECTED:
                break;
            case CONNECTION_FAILED:
            case ERROR:
            case CLOSED:
                value.put(REASON_CODE, text(update.getReasonCode(), 256));
                break;
            case ROOM_STATE:
                value.put(ROOM, encodeRoomState(update.getRoomState()));
                break;
            case MATCH_STARTED:
                value.put(ROOM_ID, id(update.getRoomId()));
                value.put(ROOM_VERSION, update.getRoomVersion());
                value.put(MATCH_ID, id(update.getMatchId()));
                value.put(LOCAL_PARTICIPANT_ID, id(update.getLocalParticipantId()));
                break;
            case SNAPSHOT:
                value.put(ROOM_ID, id(update.getRoomId()));
                value.put(MATCH_ID, id(update.getMatchId()));
                value.put(BATTLE, WireSnapshots.encode(update.getBattleState()));
                break;
            case REQUEST_OUTCOME:
                value.put(OUTCOME, encodeOutcome(update.getRequestOutcome()));
                break;
            case RANKED_SAVE_STATUS:
                value.put(ROOM_ID, id(update.getRoomId()));
                value.put(MATCH_ID, id(update.getMatchId()));
                value.put(REASON_CODE, text(update.getReasonCode(), 256));
                break;
            case EVENTS:
                value.put(ROOM_ID, id(update.getRoomId()));
                value.put(MATCH_ID, id(update.getMatchId()));
                value.put(EVENTS, WireItemEvents.encode(update.getEvents()));
                break;
            default:
                throw new IOException("Unknown network update type");
        }
        writeFrame(output, value);
    }

    public static NetworkUpdate readUpdate(InputStream input) throws IOException {
        Object raw = readFrame(input);
        if (raw == null) return null;
        try {
            Map<String, Object> value = object(raw);
            envelope(value, "update");
            NetworkUpdate.Type type = enumeration(NetworkUpdate.Type.class, value.get(TYPE));
            switch (type) {
                case CONNECTED:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE }, new String[0]);
                    return NetworkUpdate.connected();
                case CONNECTION_FAILED:
                case ERROR:
                case CLOSED:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, REASON_CODE }, new String[0]);
                    String reason = text(value.get(REASON_CODE), 256);
                    return type == NetworkUpdate.Type.CONNECTION_FAILED ? NetworkUpdate.connectionFailed(reason)
                            : type == NetworkUpdate.Type.ERROR ? NetworkUpdate.error(reason)
                            : NetworkUpdate.closed(reason);
                case ROOM_STATE:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, ROOM }, new String[0]);
                    return NetworkUpdate.roomState(decodeRoomState(value.get(ROOM)));
                case MATCH_STARTED:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, ROOM_ID, ROOM_VERSION,
                            MATCH_ID, LOCAL_PARTICIPANT_ID }, new String[0]);
                    return NetworkUpdate.matchStarted(id(value.get(ROOM_ID)), nonnegative(value.get(ROOM_VERSION)),
                            id(value.get(MATCH_ID)), id(value.get(LOCAL_PARTICIPANT_ID)));
                case SNAPSHOT:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, ROOM_ID, MATCH_ID, BATTLE },
                            new String[0]);
                    return NetworkUpdate.snapshot(id(value.get(ROOM_ID)), id(value.get(MATCH_ID)),
                            WireSnapshots.decode(value.get(BATTLE)));
                case REQUEST_OUTCOME:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, OUTCOME }, new String[0]);
                    return NetworkUpdate.requestOutcome(decodeOutcome(value.get(OUTCOME)));
                case RANKED_SAVE_STATUS:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, ROOM_ID, MATCH_ID, REASON_CODE },
                            new String[0]);
                    return NetworkUpdate.rankedSaveStatus(id(value.get(ROOM_ID)), id(value.get(MATCH_ID)),
                            text(value.get(REASON_CODE), 256));
                case EVENTS:
                    keys(value, new String[] { VERSION, MESSAGE, TYPE, ROOM_ID, MATCH_ID, EVENTS },
                            new String[0]);
                    return NetworkUpdate.events(id(value.get(ROOM_ID)), id(value.get(MATCH_ID)),
                            WireItemEvents.decode(value.get(EVENTS)));
                default:
                    throw new IOException("Unknown network update type");
            }
        } catch (RuntimeException error) {
            throw new IOException("Invalid wire update", error);
        }
    }

    private static Map<String, Object> base(String message) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(VERSION, PROTOCOL_VERSION);
        value.put(MESSAGE, message);
        return value;
    }

    private static void envelope(Map<String, Object> value, String message) throws IOException {
        if (integer(value.get(VERSION)) != PROTOCOL_VERSION || !message.equals(value.get(MESSAGE))) {
            throw new IOException("Unsupported wire envelope or protocol version");
        }
    }

    private static Map<String, Object> encodeRoomCommand(RoomCommand command) throws IOException {
        if (command == null) throw new IOException("Room command is required");
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(TYPE, command.getType().name());
        switch (command.getType()) {
            case CREATE_ROOM:
                value.put(MAX_PARTICIPANTS, command.getMaxParticipants());
                break;
            case JOIN_ROOM:
                value.put(ROOM_ID, id(command.getRoomId()));
                break;
            case SET_READY:
                value.put(READY, command.getReady());
                break;
            case LEAVE_ROOM:
            case REQUEST_REMATCH:
            case GET_ROOM_STATE:
                break;
            default:
                throw new IOException("Unknown room command");
        }
        return value;
    }

    private static RoomCommand decodeRoomCommand(Object raw) throws IOException {
        Map<String, Object> value = object(raw);
        RoomCommand.Type type = enumeration(RoomCommand.Type.class, value.get(TYPE));
        switch (type) {
            case CREATE_ROOM:
                keys(value, new String[] { TYPE, MAX_PARTICIPANTS }, new String[0]);
                return RoomCommand.createRoom(integer(value.get(MAX_PARTICIPANTS)));
            case JOIN_ROOM:
                keys(value, new String[] { TYPE, ROOM_ID }, new String[0]);
                return RoomCommand.joinRoom(id(value.get(ROOM_ID)));
            case SET_READY:
                keys(value, new String[] { TYPE, READY }, new String[0]);
                return RoomCommand.setReady(bool(value.get(READY)));
            case LEAVE_ROOM:
            case REQUEST_REMATCH:
            case GET_ROOM_STATE:
                keys(value, new String[] { TYPE }, new String[0]);
                return type == RoomCommand.Type.LEAVE_ROOM ? RoomCommand.leaveRoom()
                        : type == RoomCommand.Type.REQUEST_REMATCH ? RoomCommand.requestRematch()
                        : RoomCommand.getRoomState();
            default:
                throw new IOException("Unknown room command");
        }
    }

    private static Map<String, Object> encodeIntent(PlayerIntent intent) throws IOException {
        if (intent == null) throw new IOException("Player intent is required");
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(TYPE, intent.getType().name());
        if (intent.getItemUse() != null) {
            GameAction.ItemUse item = intent.getItemUse();
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put(ITEM_ID, id(item.getItemId()));
            payload.put(TARGET_ACTOR_ID, id(item.getTargetActorId()));
            if (item.getTargetCell() != null) {
                Map<String, Object> cell = new LinkedHashMap<String, Object>();
                cell.put("x", item.getTargetCell().getX());
                cell.put("y", item.getTargetCell().getY());
                payload.put(TARGET_CELL, cell);
            }
            value.put(ITEM_USE, payload);
        }
        return value;
    }

    private static PlayerIntent decodeIntent(Object raw) throws IOException {
        Map<String, Object> value = object(raw);
        GameAction.Type type = enumeration(GameAction.Type.class, value.get(TYPE));
        if (type != GameAction.Type.USE_ITEM) {
            keys(value, new String[] { TYPE }, new String[0]);
            return new PlayerIntent(type);
        }
        keys(value, new String[] { TYPE, ITEM_USE }, new String[0]);
        Map<String, Object> payload = object(value.get(ITEM_USE));
        keys(payload, new String[] { ITEM_ID, TARGET_ACTOR_ID }, new String[] { TARGET_CELL });
        GameAction.TargetCell cell = null;
        if (payload.containsKey(TARGET_CELL)) {
            Map<String, Object> target = object(payload.get(TARGET_CELL));
            keys(target, new String[] { "x", "y" }, new String[0]);
            cell = new GameAction.TargetCell(integer(target.get("x")), integer(target.get("y")));
        }
        return new PlayerIntent(type, new GameAction.ItemUse(id(payload.get(ITEM_ID)),
                id(payload.get(TARGET_ACTOR_ID)), cell));
    }

    private static Map<String, Object> encodeRoomState(RoomState state) throws IOException {
        if (state == null) throw new IOException("Room state is required");
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(ROOM_ID, id(state.getRoomId()));
        value.put(ROOM_VERSION, state.getRoomVersion());
        value.put(PHASE, state.getPhase().name());
        value.put(LOCAL_PARTICIPANT_ID, id(state.getLocalParticipantId()));
        Map<String, Object> ready = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Boolean> entry : state.getReadyByParticipantId().entrySet()) {
            ready.put(id(entry.getKey()), entry.getValue());
        }
        value.put(READY, ready);
        return value;
    }

    private static RoomState decodeRoomState(Object raw) throws IOException {
        Map<String, Object> value = object(raw);
        keys(value, new String[] { ROOM_ID, ROOM_VERSION, PHASE, LOCAL_PARTICIPANT_ID, READY },
                new String[0]);
        Map<String, Object> rawReady = object(value.get(READY));
        Map<String, Boolean> ready = new LinkedHashMap<String, Boolean>();
        for (Map.Entry<String, Object> entry : rawReady.entrySet()) {
            ready.put(id(entry.getKey()), bool(entry.getValue()));
        }
        return new RoomState(id(value.get(ROOM_ID)), nonnegative(value.get(ROOM_VERSION)),
                enumeration(RoomState.Phase.class, value.get(PHASE)),
                id(value.get(LOCAL_PARTICIPANT_ID)), ready);
    }

    private static Map<String, Object> encodeOutcome(RequestOutcome outcome) throws IOException {
        if (outcome == null) throw new IOException("Request outcome is required");
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put(REQUEST_ID, outcome.getRequestId());
        value.put(ACCEPTED, outcome.isAccepted());
        if (outcome.getReasonCode() != null) value.put(REASON_CODE, text(outcome.getReasonCode(), 256));
        if (outcome.getRoomId() != null) value.put(ROOM_ID, id(outcome.getRoomId()));
        if (outcome.getMatchId() != null) value.put(MATCH_ID, id(outcome.getMatchId()));
        if (outcome.getBattleVersion() != null) value.put(BATTLE_VERSION, outcome.getBattleVersion());
        return value;
    }

    private static RequestOutcome decodeOutcome(Object raw) throws IOException {
        Map<String, Object> value = object(raw);
        keys(value, new String[] { REQUEST_ID, ACCEPTED },
                new String[] { REASON_CODE, ROOM_ID, MATCH_ID, BATTLE_VERSION });
        Long battleVersion = value.containsKey(BATTLE_VERSION)
                ? Long.valueOf(nonnegative(value.get(BATTLE_VERSION))) : null;
        return new RequestOutcome(positive(value.get(REQUEST_ID)), bool(value.get(ACCEPTED)),
                optionalText(value, REASON_CODE, 256), optionalId(value, ROOM_ID),
                optionalId(value, MATCH_ID), battleVersion);
    }

    private static void writeFrame(OutputStream output, Object value) throws IOException {
        if (output == null) throw new IOException("Output stream is required");
        byte[] payload = StrictJson.stringify(value);
        int length = payload.length;
        if (length < 1 || length > MAX_FRAME_BYTES) throw new IOException("Wire frame length exceeds limit");
        output.write((length >>> 24) & 0xff);
        output.write((length >>> 16) & 0xff);
        output.write((length >>> 8) & 0xff);
        output.write(length & 0xff);
        output.write(payload);
        output.flush();
    }

    private static Object readFrame(InputStream input) throws IOException {
        if (input == null) throw new IOException("Input stream is required");
        int first = input.read();
        if (first == -1) return null;
        int second = input.read();
        int third = input.read();
        int fourth = input.read();
        if (second == -1 || third == -1 || fourth == -1) throw new EOFException("Truncated wire header");
        long length = ((long) first << 24) | ((long) second << 16) | ((long) third << 8) | fourth;
        if (length < 1 || length > MAX_FRAME_BYTES) throw new IOException("Invalid wire frame length");
        byte[] payload = new byte[(int) length];
        int offset = 0;
        while (offset < payload.length) {
            int count = input.read(payload, offset, payload.length - offset);
            if (count == -1) throw new EOFException("Truncated wire payload");
            if (count == 0) {
                int next = input.read();
                if (next == -1) throw new EOFException("Truncated wire payload");
                payload[offset++] = (byte) next;
            } else offset += count;
        }
        return StrictJson.parse(payload);
    }

    static Map<String, Object> object(Object raw) throws IOException {
        if (!(raw instanceof Map)) throw new IOException("JSON object is required");
        @SuppressWarnings("unchecked") Map<String, Object> value = (Map<String, Object>) raw;
        return value;
    }

    static List<Object> array(Object raw) throws IOException {
        if (!(raw instanceof List)) throw new IOException("JSON array is required");
        @SuppressWarnings("unchecked") List<Object> value = (List<Object>) raw;
        return value;
    }

    static void keys(Map<String, Object> value, String[] required, String[] optional) throws IOException {
        for (String key : required) {
            if (!value.containsKey(key)) throw new IOException("Missing field: " + key);
        }
        for (String key : value.keySet()) {
            if (!contains(required, key) && !contains(optional, key)) {
                throw new IOException("Unknown wire field: " + key);
            }
        }
    }

    private static boolean contains(String[] names, String key) {
        for (String name : names) {
            if (key.equals(name)) return true;
        }
        return false;
    }

    static String text(Object raw, int maxChars) throws IOException {
        if (!(raw instanceof String)) throw new IOException("JSON text is required");
        String value = (String) raw;
        if (value.trim().isEmpty() || value.length() > maxChars) throw new IOException("Invalid wire text length");
        return value;
    }

    static String id(Object raw) throws IOException {
        String value = text(raw, 128);
        if (!value.matches("[A-Za-z0-9_.:-]{1,128}")) throw new IOException("Invalid wire identifier");
        return value;
    }

    static String optionalText(Map<String, Object> value, String key, int maxChars) throws IOException {
        return !value.containsKey(key) || value.get(key) == null ? null : text(value.get(key), maxChars);
    }

    static String optionalId(Map<String, Object> value, String key) throws IOException {
        return !value.containsKey(key) || value.get(key) == null ? null : id(value.get(key));
    }

    static boolean bool(Object raw) throws IOException {
        if (!(raw instanceof Boolean)) throw new IOException("JSON boolean is required");
        return ((Boolean) raw).booleanValue();
    }

    static long nonnegative(Object raw) throws IOException {
        if (!(raw instanceof Long) || ((Long) raw).longValue() < 0) {
            throw new IOException("Nonnegative integer is required");
        }
        return ((Long) raw).longValue();
    }

    static long positive(Object raw) throws IOException {
        long value = nonnegative(raw);
        if (value == 0) throw new IOException("Positive integer is required");
        return value;
    }

    static int integer(Object raw) throws IOException {
        if (!(raw instanceof Long)) throw new IOException("JSON integer is required");
        long value = ((Long) raw).longValue();
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IOException("Integer outside supported range");
        }
        return (int) value;
    }

    static <E extends Enum<E>> E enumeration(Class<E> enumClass, Object raw) throws IOException {
        String name = text(raw, 64);
        try { return Enum.valueOf(enumClass, name); }
        catch (IllegalArgumentException error) { throw new IOException("Unknown wire enum value: " + name, error); }
    }
}
