package kr.ac.jbnu.se.tetris.network.server;

import java.io.IOException;
import java.net.Socket;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;
import kr.ac.jbnu.se.tetris.network.RequestOutcome;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.network.protocol.WireCodec;
import kr.ac.jbnu.se.tetris.network.protocol.WireRequest;

/** 두 실제 소켓의 방·입력·승자·재대결 통합 검증 */
public final class LocalGameServerTest {
    public static void main(String[] args) throws Exception {
        matchIdentityAndLeave();
        disconnectForfeit();
        finishedMatchRematch();
    }

    private static void matchIdentityAndLeave() throws Exception {
        LocalGameServer server = new LocalGameServer(0);
        server.start();
        check(server.getPort() > 0, "Ephemeral loopback port");
        Client first = new Client(server.getPort());
        Client second = new Client(server.getPort());
        try {
            first.send(WireRequest.room(1, RoomCommand.createRoom(4)));
            first.outcome(1, false, "ROOM_SIZE_NOT_SUPPORTED");
            first.send(WireRequest.room(2, RoomCommand.createRoom(2)));
            first.outcome(2, true, null);
            RoomState created = first.roomState();
            String roomId = created.getRoomId();
            String firstId = created.getLocalParticipantId();
            check(created.getReadyByParticipantId().size() == 1, "Room starts with one assigned ID");

            second.send(WireRequest.room(1, RoomCommand.joinRoom(roomId)));
            second.outcome(1, true, null);
            RoomState joined = second.roomState();
            String secondId = joined.getLocalParticipantId();
            check(!firstId.equals(secondId) && joined.getReadyByParticipantId().size() == 2
                    && joined.getReadyByParticipantId().containsKey(firstId),
                    "Distinct connection-bound participant IDs");

            first.send(WireRequest.room(3, RoomCommand.setReady(true)));
            first.outcome(3, true, null);
            second.send(WireRequest.room(2, RoomCommand.setReady(true)));
            second.outcome(2, true, null);
            NetworkUpdate firstStart = first.matchStarted();
            NetworkUpdate secondStart = second.matchStarted();
            String matchId = firstStart.getMatchId();
            check(matchId.equals(secondStart.getMatchId())
                    && firstId.equals(firstStart.getLocalParticipantId())
                    && secondId.equals(secondStart.getLocalParticipantId()),
                    "Same match with each local identity");
            check(first.snapshot(matchId, -1).getBattleState().getStatus() == BattleState.Status.RUNNING,
                    "First initial snapshot");
            check(second.snapshot(matchId, -1).getBattleState().getStatus() == BattleState.Status.RUNNING,
                    "Second initial snapshot");

            first.send(WireRequest.intent(4, matchId, new PlayerIntent(GameAction.Type.MOVE_LEFT)));
            RequestOutcome moved = first.outcome(4, true, null);
            check(moved.getBattleVersion() != null, "Accepted input has battle version");
            long version = moved.getBattleVersion().longValue();
            BattleState a = first.snapshot(matchId, version).getBattleState();
            BattleState b = second.snapshot(matchId, version).getBattleState();
            sameState(a, b);
            check(a.getParticipant(firstId).getGameState().getPieceX()
                    == b.getParticipant(firstId).getGameState().getPieceX(),
                    "Input by socket identity reaches both peers");

            first.send(WireRequest.intent(4, matchId, new PlayerIntent(GameAction.Type.HARD_DROP)));
            first.outcome(4, false, "REQUEST_ID_NOT_INCREASING");
            first.send(WireRequest.intent(5, "old-match", new PlayerIntent(GameAction.Type.HARD_DROP)));
            first.outcome(5, false, "STALE_MATCH");
            first.send(WireRequest.snapshot(6, matchId));
            first.outcome(6, true, null);
            first.snapshot(matchId, -1);

            first.send(WireRequest.room(7, RoomCommand.leaveRoom()));
            first.outcome(7, true, null);
            check(first.roomState().getPhase() == RoomState.Phase.CLOSED,
                    "Leaver receives a closed room state");
            BattleState finished = second.finished(matchId).getBattleState();
            check(finished.getStatus() == BattleState.Status.FINISHED
                    && secondId.equals(finished.getWinnerId())
                    && "FORFEIT".equals(finished.getReason()),
                    "Leave creates authoritative forfeit result");
        } finally {
            first.close(); second.close(); server.close(); server.close();
        }
    }

    private static void disconnectForfeit() throws Exception {
        LocalGameServer server = new LocalGameServer(0);
        server.start();
        Client first = new Client(server.getPort());
        Client second = new Client(server.getPort());
        try {
            String matchId = startTwoPlayerMatch(first, second);
            String survivor = second.localId;
            first.close();
            BattleState finished = second.finished(matchId).getBattleState();
            check(survivor.equals(finished.getWinnerId())
                    && "FORFEIT".equals(finished.getReason()), "Disconnect forfeits only departed ID");
        } finally {
            first.close(); second.close(); server.close();
        }
    }

    private static void finishedMatchRematch() throws Exception {
        LocalGameServer server = new LocalGameServer(0);
        server.start();
        Client first = new Client(server.getPort());
        Client second = new Client(server.getPort());
        try {
            String oldMatch = startTwoPlayerMatch(first, second);
            long requestId = 3;
            BattleState finished = null;
            for (int attempt = 0; attempt < 40 && finished == null; attempt++) {
                first.send(WireRequest.intent(requestId, oldMatch,
                        new PlayerIntent(GameAction.Type.HARD_DROP)));
                RequestOutcome outcome = first.anyOutcome(requestId++);
                if (!outcome.isAccepted() && "Waiting for next gravity tick to spawn".equals(outcome.getReasonCode())) {
                    Thread.sleep(420);
                } else if (outcome.isAccepted() && outcome.getBattleVersion() != null) {
                    BattleState state = first.snapshot(oldMatch,
                            outcome.getBattleVersion().longValue()).getBattleState();
                    if (state.getStatus() == BattleState.Status.FINISHED) finished = state;
                } else if (!outcome.isAccepted() && "MATCH_NOT_RUNNING".equals(outcome.getReasonCode())) {
                    // 중력 tick이 다음 입력보다 먼저 종료한 경우 서버의 확정 상태 재조회
                    first.send(WireRequest.snapshot(requestId, oldMatch));
                    first.outcome(requestId++, true, null);
                    finished = first.snapshot(oldMatch, -1).getBattleState();
                    check(finished.getStatus() == BattleState.Status.FINISHED,
                            "Gravity termination is confirmed by an authoritative snapshot");
                } else if (!outcome.isAccepted()) {
                    throw new AssertionError("Unexpected hard drop rejection: " + outcome.getReasonCode());
                }
            }
            check(finished != null, "Real input reaches a finished match");
            second.finished(oldMatch);

            first.send(WireRequest.intent(requestId, oldMatch,
                    new PlayerIntent(GameAction.Type.HARD_DROP)));
            first.outcome(requestId++, false, "MATCH_NOT_RUNNING");

            first.send(WireRequest.room(requestId++, RoomCommand.requestRematch()));
            first.outcome(requestId - 1, true, null);
            second.send(WireRequest.room(3, RoomCommand.requestRematch()));
            second.outcome(3, true, null);
            String nextA = first.matchStarted().getMatchId();
            String nextB = second.matchStarted().getMatchId();
            check(!oldMatch.equals(nextA) && nextA.equals(nextB), "Rematch has a new match ID");
            BattleState initial = first.snapshot(nextA, -1).getBattleState();
            check(initial.getStatus() == BattleState.Status.RUNNING
                    && initial.getParticipant(first.localId).getHp() == 100
                    && initial.getParticipant(second.localId).getHp() == 100,
                    "Rematch resets HP and board state");
            second.snapshot(nextB, -1);
            first.send(WireRequest.intent(requestId, oldMatch,
                    new PlayerIntent(GameAction.Type.MOVE_LEFT)));
            first.outcome(requestId, false, "STALE_MATCH");
        } finally {
            first.close(); second.close(); server.close();
        }
    }

    private static String startTwoPlayerMatch(Client first, Client second) throws Exception {
        first.send(WireRequest.room(1, RoomCommand.createRoom(2)));
        first.outcome(1, true, null);
        String roomId = first.roomState().getRoomId();
        second.send(WireRequest.room(1, RoomCommand.joinRoom(roomId)));
        second.outcome(1, true, null);
        second.roomState();
        first.send(WireRequest.room(2, RoomCommand.setReady(true)));
        first.outcome(2, true, null);
        second.send(WireRequest.room(2, RoomCommand.setReady(true)));
        second.outcome(2, true, null);
        String match = first.matchStarted().getMatchId();
        check(match.equals(second.matchStarted().getMatchId()), "Both peers share match ID");
        first.snapshot(match, -1);
        second.snapshot(match, -1);
        return match;
    }

    private static void sameState(BattleState left, BattleState right) {
        check(left.getVersion() == right.getVersion()
                && left.getStatus() == right.getStatus()
                && left.getParticipants().keySet().equals(right.getParticipants().keySet()),
                "Peers receive one authoritative version");
        for (String id : left.getParticipants().keySet()) {
            ParticipantState a = left.getParticipant(id), b = right.getParticipant(id);
            check(a.getHp() == b.getHp()
                    && a.getGameState().getVersion() == b.getGameState().getVersion()
                    && a.getGameState().getPieceX() == b.getGameState().getPieceX()
                    && a.getGameState().getPieceY() == b.getGameState().getPieceY(),
                    "Peer participant state equality");
            for (int y = 0; y < a.getGameState().getBoard().getHeight(); y++) {
                for (int x = 0; x < a.getGameState().getBoard().getWidth(); x++) {
                    check(a.getGameState().getBoard().getCell(x, y)
                            == b.getGameState().getBoard().getCell(x, y), "Peer board equality");
                }
            }
        }
    }

    private interface UpdatePredicate { boolean accepts(NetworkUpdate update); }

    private static final class Client implements AutoCloseable {
        private final Socket socket;
        private String localId;

        private Client(int port) throws Exception {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(5000);
            check(next().getType() == NetworkUpdate.Type.CONNECTED, "Connected event");
        }

        private void send(WireRequest request) throws IOException {
            WireCodec.writeRequest(socket.getOutputStream(), request);
        }

        private NetworkUpdate next() throws IOException {
            NetworkUpdate update = WireCodec.readUpdate(socket.getInputStream());
            if (update == null) throw new AssertionError("Unexpected server disconnect");
            return update;
        }

        private NetworkUpdate until(UpdatePredicate predicate) throws IOException {
            for (int count = 0; count < 512; count++) {
                NetworkUpdate update = next();
                if (predicate.accepts(update)) return update;
            }
            throw new AssertionError("Expected server update not received");
        }

        private RequestOutcome outcome(final long requestId, boolean accepted, String reason) throws IOException {
            RequestOutcome outcome = anyOutcome(requestId);
            check(outcome.isAccepted() == accepted && same(reason, outcome.getReasonCode()),
                    "Request outcome " + requestId + ": " + outcome.getReasonCode());
            return outcome;
        }

        private RequestOutcome anyOutcome(final long requestId) throws IOException {
            return until(new UpdatePredicate() {
                public boolean accepts(NetworkUpdate update) {
                    return update.getType() == NetworkUpdate.Type.REQUEST_OUTCOME
                            && update.getRequestOutcome().getRequestId() == requestId;
                }
            }).getRequestOutcome();
        }

        private RoomState roomState() throws IOException {
            RoomState state = until(new UpdatePredicate() {
                public boolean accepts(NetworkUpdate update) {
                    return update.getType() == NetworkUpdate.Type.ROOM_STATE;
                }
            }).getRoomState();
            localId = state.getLocalParticipantId();
            return state;
        }

        private NetworkUpdate matchStarted() throws IOException {
            NetworkUpdate update = until(new UpdatePredicate() {
                public boolean accepts(NetworkUpdate update) {
                    return update.getType() == NetworkUpdate.Type.MATCH_STARTED;
                }
            });
            localId = update.getLocalParticipantId();
            return update;
        }

        private NetworkUpdate snapshot(final String matchId, final long version) throws IOException {
            return until(new UpdatePredicate() {
                public boolean accepts(NetworkUpdate update) {
                    return update.getType() == NetworkUpdate.Type.SNAPSHOT
                            && matchId.equals(update.getMatchId())
                            && (version < 0 || update.getBattleState().getVersion() == version);
                }
            });
        }

        private NetworkUpdate finished(final String matchId) throws IOException {
            return until(new UpdatePredicate() {
                public boolean accepts(NetworkUpdate update) {
                    return update.getType() == NetworkUpdate.Type.SNAPSHOT
                            && matchId.equals(update.getMatchId())
                            && update.getBattleState().getStatus() == BattleState.Status.FINISHED;
                }
            });
        }

        private static boolean same(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }

        @Override public void close() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
