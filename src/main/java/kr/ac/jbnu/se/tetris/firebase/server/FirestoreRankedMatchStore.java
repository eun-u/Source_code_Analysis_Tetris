package kr.ac.jbnu.se.tetris.firebase.server;

import static kr.ac.jbnu.se.tetris.firebase.server.FirestoreSchema.*;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import kr.ac.jbnu.se.tetris.auth.UsernameIdentity;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardEntry;
import kr.ac.jbnu.se.tetris.ranking.MatchRecord;
import kr.ac.jbnu.se.tetris.ranking.RankedMatchStore;
import kr.ac.jbnu.se.tetris.ranking.RunLease;

/** Transactional ranked storage. Only the Cloud Run service identity may write these collections. */
public final class FirestoreRankedMatchStore implements RankedMatchStore {
    private static final long LEASE_MILLIS = 30_000;
    // Recovery must be atomic with fencing. Refuse oversized recovery rather than leave half a run active.
    private static final int MAX_RECOVERY_MATCHES = 100;
    // 서버가 클라이언트 응답과 로그에 그대로 쓰는 오류 코드
    private static final String INVALID_MATCH = "INVALID_MATCH";
    private static final String UNKNOWN_MATCH = "UNKNOWN_MATCH";
    private static final String INVALID_RUN = "INVALID_RUN";
    private static final String RUN_NOT_OWNER = "RUN_NOT_OWNER";
    private static final String ACTIVE_MATCH_CONFLICT = "ACTIVE_MATCH_CONFLICT";
    private static final String RUN_STOPPED = "RUN_STOPPED";
    private static final String RUN_MISMATCH = "RUN_MISMATCH";
    private static final String INVALID_REASON = "INVALID_REASON";
    private final Firestore firestore;
    private final FirebaseAuth auth;
    private final Clock clock;
    private final DocumentReference control;
    private final CollectionReference runs;
    private final CollectionReference matches;
    private final CollectionReference players;

    public FirestoreRankedMatchStore(Firestore firestore) { this(firestore, null, Clock.systemUTC()); }
    public FirestoreRankedMatchStore(Firestore firestore, FirebaseAuth auth) {
        this(firestore, auth, Clock.systemUTC());
    }

    /** Clock injection is used by the emulator integration test; production uses Cloud Run's synced clock. */
    public FirestoreRankedMatchStore(Firestore firestore, FirebaseAuth auth, Clock clock) {
        if (firestore == null || clock == null) throw new IllegalArgumentException("Firestore and clock required");
        this.firestore = firestore;
        this.auth = auth;
        this.clock = clock;
        this.control = firestore.collection(RANKED_CONTROL).document(CONTROL_DOCUMENT);
        this.runs = firestore.collection(RANKED_RUNS);
        this.matches = firestore.collection(RANKED_MATCHES);
        this.players = firestore.collection(RANKED_PLAYERS);
    }

    @Override public RunLease claimRun(String serverRunId) throws IOException {
        return lease(serverRunId, Action.CLAIM);
    }

    @Override public RunLease renewRun(String serverRunId) throws IOException {
        return lease(serverRunId, Action.RENEW);
    }

    @Override public RunLease setAdmission(String serverRunId, boolean enabled) throws IOException {
        return lease(serverRunId, enabled ? Action.OPEN : Action.DRAIN);
    }

    private enum Action { CLAIM, RENEW, OPEN, DRAIN }

    private RunLease lease(String runId, Action action) throws IOException {
        required(runId, INVALID_RUN);
        return transaction(tx -> leaseInTransaction(tx, runId, action));
    }

    /** 임대 갱신·접수 변경·신규 실행 인수를 한 트랜잭션에서 판정한다. */
    private RunLease leaseInTransaction(Transaction tx, String runId, Action action) throws Exception {
        long now = clock.millis();
        DocumentSnapshot gate = await(tx.get(control));
        boolean ownerOnly = action == Action.RENEW || action == Action.DRAIN;
        // 새 실행 인수와 접수 개방은 이관이 끝난 뒤에만 허용한다.
        if (!ownerOnly) assertMigrationComplete(tx);
        String owner = string(gate, OWNER_RUN_ID);
        long until = number(gate, LEASE_UNTIL_MILLIS, 0);
        boolean enabled = bool(gate, ENABLED);
        boolean leaseLive = until > now;
        boolean nextEnabled = ownerOnly ? action != Action.DRAIN && enabled : action == Action.OPEN || enabled;
        if (ownerOnly || runId.equals(owner)) {
            // 이미 소유한 실행만 연장할 수 있고, 만료된 자기 임대는 갱신하지 않는다.
            return runId.equals(owner) && leaseLive ? extendLease(tx, runId, now, nextEnabled)
                    : new RunLease(false, enabled);
        }
        if (owner != null && leaseLive) return new RunLease(false, enabled);
        if (action == Action.CLAIM && !enabled) return new RunLease(false, false);
        return takeOverLease(tx, runId, owner, now, nextEnabled);
    }

    private void assertMigrationComplete(Transaction tx) throws Exception {
        DocumentSnapshot migration = await(tx.get(firestore.collection(MIGRATION_STATE).document(MIGRATION_DOCUMENT)));
        if (migration.exists() && !MARKER_COMPLETE.equals(migration.getString(STATUS))) {
            throw invalid("MIGRATION_IN_PROGRESS");
        }
    }

    private RunLease extendLease(Transaction tx, String runId, long now, boolean nextEnabled) {
        tx.set(control, controlFields(runId, now + LEASE_MILLIS, nextEnabled));
        return new RunLease(true, nextEnabled);
    }

    /** 만료된 이전 실행을 정지하고 그 실행의 진행 중 경기를 무효 처리한 뒤 임대를 가져온다. */
    private RunLease takeOverLease(Transaction tx, String runId, String owner, long now, boolean nextEnabled)
            throws Exception {
        DocumentReference runRef = runs.document(runId);
        DocumentSnapshot run = await(tx.get(runRef));
        if (STATE_STOPPED.equals(string(run, STATE))) throw invalid(RUN_STOPPED);
        QuerySnapshot active = await(tx.get(runs.whereEqualTo(STATE, STATE_ACTIVE)));
        Set<String> stale = new LinkedHashSet<>();
        for (QueryDocumentSnapshot candidate : active.getDocuments()) {
            if (!runId.equals(candidate.getId())) stale.add(candidate.getId());
        }
        if (owner != null && !runId.equals(owner)) stale.add(owner);
        Recovery recovery = readRecovery(tx, stale);

        writeRecovery(tx, recovery, now);
        if (!run.exists()) tx.set(runRef, runFields(STATE_ACTIVE, now, null));
        return extendLease(tx, runId, now, nextEnabled);
    }

    @Override public MatchRecord beginMatch(String matchId, String serverRunId, String rulesVersion,
            String firstUserId, String secondUserId) throws IOException {
        required(matchId, INVALID_MATCH);
        required(serverRunId, INVALID_MATCH);
        required(firstUserId, INVALID_MATCH);
        required(secondUserId, INVALID_MATCH);
        bounded(rulesVersion, INVALID_MATCH);
        if (firstUserId.equals(secondUserId)) throw new IOException(INVALID_MATCH);
        // Auth lookups happen outside retryable Firestore callbacks; the transaction still checks active slots.
        String firstName = profileName(firstUserId);
        String secondName = profileName(secondUserId);
        DocumentReference matchRef = matches.document(matchId);
        DocumentReference firstRef = player(firstUserId);
        DocumentReference secondRef = player(secondUserId);
        return transaction(tx -> {
            long now = clock.millis();
            DocumentSnapshot gate = await(tx.get(control));
            assertLease(gate, serverRunId, true, now);
            DocumentSnapshot run = await(tx.get(runs.document(serverRunId)));
            if (!STATE_ACTIVE.equals(string(run, STATE))) throw invalid(RUN_STOPPED);
            DocumentSnapshot existing = await(tx.get(matchRef));
            if (existing.exists()) {
                if (!serverRunId.equals(string(existing, SERVER_RUN_ID))
                        || !rulesVersion.equals(string(existing, RULES_VERSION))
                        || !firstUserId.equals(string(existing, FIRST_USER_ID))
                        || !secondUserId.equals(string(existing, SECOND_USER_ID)))
                    throw invalid("MATCH_ID_CONFLICT");
                return record(existing);
            }
            DocumentSnapshot first = await(tx.get(firstRef));
            DocumentSnapshot second = await(tx.get(secondRef));
            if (string(first, ACTIVE_MATCH_ID) != null || string(second, ACTIVE_MATCH_ID) != null)
                throw invalid("ACCOUNT_IN_ACTIVE_MATCH");
            Map<String, Object> firstData = playerData(first, firstUserId, firstName);
            Map<String, Object> secondData = playerData(second, secondUserId, secondName);
            firstData.put(ACTIVE_MATCH_ID, matchId);
            secondData.put(ACTIVE_MATCH_ID, matchId);
            Map<String, Object> game = new HashMap<>();
            game.put(MATCH_ID, matchId);
            game.put(SERVER_RUN_ID, serverRunId);
            game.put(RULES_VERSION, rulesVersion);
            game.put(FIRST_USER_ID, firstUserId);
            game.put(SECOND_USER_ID, secondUserId);
            game.put(STATUS, MATCH_RUNNING);
            game.put(WINNER_USER_ID, null);
            game.put(REASON, null);
            game.put(STARTED_AT_MILLIS, now);
            game.put(ENDED_AT_MILLIS, null);
            game.put(FIRST_RATING_BEFORE, null);
            game.put(FIRST_RATING_AFTER, null);
            game.put(SECOND_RATING_BEFORE, null);
            game.put(SECOND_RATING_AFTER, null);
            tx.set(matchRef, game);
            tx.set(firstRef, firstData);
            tx.set(secondRef, secondData);
            return record(game);
        });
    }

    @Override public MatchRecord finishMatch(String matchId, String serverRunId,
            String winnerUserId, String reason) throws IOException {
        required(matchId, UNKNOWN_MATCH);
        required(serverRunId, RUN_NOT_OWNER);
        bounded(reason, INVALID_REASON);
        DocumentReference matchRef = matches.document(matchId);
        return transaction(tx -> finishInTransaction(tx, matchRef, serverRunId, winnerUserId, reason));
    }

    private MatchRecord finishInTransaction(Transaction tx, DocumentReference matchRef, String serverRunId,
            String winnerUserId, String reason) throws Exception {
        long now = clock.millis();
        DocumentSnapshot gate = await(tx.get(control));
        assertLease(gate, serverRunId, false, now);
        DocumentSnapshot match = await(tx.get(matchRef));
        if (!match.exists()) throw invalid(UNKNOWN_MATCH);
        if (!serverRunId.equals(string(match, SERVER_RUN_ID))) throw invalid(RUN_MISMATCH);
        String status = string(match, STATUS);
        if (MATCH_FINALIZED.equals(status)) {
            // 같은 결과의 재요청만 멱등 성공으로 돌려준다.
            if (sameResult(match, winnerUserId, reason)) return record(match);
            throw invalid("RESULT_CONFLICT");
        }
        if (!MATCH_RUNNING.equals(status)) throw invalid("MATCH_NOT_RUNNING");
        String firstId = string(match, FIRST_USER_ID);
        String secondId = string(match, SECOND_USER_ID);
        if (winnerUserId == null || !(winnerUserId.equals(firstId) || winnerUserId.equals(secondId))) {
            throw invalid("INVALID_WINNER");
        }
        DocumentReference firstRef = player(firstId);
        DocumentReference secondRef = player(secondId);
        DocumentSnapshot first = await(tx.get(firstRef));
        DocumentSnapshot second = await(tx.get(secondRef));
        if (!first.exists() || !second.exists()) throw invalid("MISSING_STATS");
        assertBothInMatch(matchRef.getId(), first, second);
        boolean firstWon = winnerUserId.equals(firstId);
        boolean secondWon = winnerUserId.equals(secondId);
        int firstRating = intValue(first, RATING, 1000);
        int secondRating = intValue(second, RATING, 1000);
        int delta = firstWon ? eloDelta(firstRating, secondRating) : eloDelta(secondRating, firstRating);
        int firstAfter = firstRating + (firstWon ? delta : -delta);
        int secondAfter = secondRating + (secondWon ? delta : -delta);
        Map<String, Object> game = copy(match);
        game.put(STATUS, MATCH_FINALIZED);
        game.put(WINNER_USER_ID, winnerUserId);
        game.put(REASON, reason);
        game.put(ENDED_AT_MILLIS, now);
        game.put(FIRST_RATING_BEFORE, firstRating);
        game.put(FIRST_RATING_AFTER, firstAfter);
        game.put(SECOND_RATING_BEFORE, secondRating);
        game.put(SECOND_RATING_AFTER, secondAfter);
        tx.set(matchRef, game);
        tx.set(firstRef, settledPlayer(first, firstAfter, firstWon));
        tx.set(secondRef, settledPlayer(second, secondAfter, secondWon));
        return record(game);
    }

    private static boolean sameResult(DocumentSnapshot match, String winnerUserId, String reason) {
        return winnerUserId != null && winnerUserId.equals(string(match, WINNER_USER_ID))
                && reason.equals(string(match, REASON));
    }

    private static void assertBothInMatch(String matchId, DocumentSnapshot first, DocumentSnapshot second) {
        if (!matchId.equals(string(first, ACTIVE_MATCH_ID)) || !matchId.equals(string(second, ACTIVE_MATCH_ID))) {
            throw invalid(ACTIVE_MATCH_CONFLICT);
        }
    }

    @Override public MatchRecord voidMatch(String matchId, String serverRunId, String reason)
            throws IOException {
        required(matchId, UNKNOWN_MATCH);
        required(serverRunId, RUN_NOT_OWNER);
        bounded(reason, INVALID_REASON);
        DocumentReference matchRef = matches.document(matchId);
        return transaction(tx -> {
            long now = clock.millis();
            DocumentSnapshot gate = await(tx.get(control));
            assertLease(gate, serverRunId, false, now);
            DocumentSnapshot match = await(tx.get(matchRef));
            if (!match.exists()) throw invalid(UNKNOWN_MATCH);
            if (!serverRunId.equals(string(match, SERVER_RUN_ID))) throw invalid(RUN_MISMATCH);
            String status = string(match, STATUS);
            if (MATCH_VOID.equals(status)) {
                if (reason.equals(string(match, REASON))) return record(match);
                throw invalid("VOID_CONFLICT");
            }
            if (!MATCH_RUNNING.equals(status)) throw invalid("MATCH_ALREADY_FINALIZED");
            DocumentReference firstRef = player(string(match, FIRST_USER_ID));
            DocumentReference secondRef = player(string(match, SECOND_USER_ID));
            DocumentSnapshot first = await(tx.get(firstRef)), second = await(tx.get(secondRef));
            if (!matchId.equals(string(first, ACTIVE_MATCH_ID))
                    || !matchId.equals(string(second, ACTIVE_MATCH_ID)))
                throw invalid(ACTIVE_MATCH_CONFLICT);
            Map<String, Object> game = copy(match);
            game.put(STATUS, MATCH_VOID);
            game.put(REASON, reason);
            game.put(ENDED_AT_MILLIS, now);
            Map<String, Object> firstData = copy(first), secondData = copy(second);
            firstData.put(ACTIVE_MATCH_ID, null);
            secondData.put(ACTIVE_MATCH_ID, null);
            tx.set(matchRef, game);
            tx.set(firstRef, firstData);
            tx.set(secondRef, secondData);
            return record(game);
        });
    }

    @Override public MatchRecord getMatch(String matchId) throws IOException {
        required(matchId, UNKNOWN_MATCH);
        DocumentSnapshot snapshot = await(matches.document(matchId).get());
        return snapshot.exists() ? record(snapshot) : null;
    }

    @Override public int voidStoppedRun(String currentRunId, String stoppedRunId) throws IOException {
        required(currentRunId, INVALID_RUN);
        required(stoppedRunId, INVALID_RUN);
        if (currentRunId.equals(stoppedRunId)) throw new IOException(INVALID_RUN);
        return transaction(tx -> {
            long now = clock.millis();
            DocumentSnapshot gate = await(tx.get(control));
            assertLease(gate, currentRunId, false, now);
            Recovery recovery = readRecovery(tx, Collections.singleton(stoppedRunId));
            writeRecovery(tx, recovery, now);
            return recovery.games.size();
        });
    }

    public List<LeaderboardEntry> top100() throws IOException {
        Query query = players.whereEqualTo(RANKED, true).orderBy(RATING, Query.Direction.DESCENDING)
                .orderBy(USER_ID, Query.Direction.ASCENDING).limit(100);
        QuerySnapshot snapshot = await(query.get());
        List<LeaderboardEntry> results = new ArrayList<>();
        int rank = 0, previousRating = Integer.MIN_VALUE;
        for (QueryDocumentSnapshot player : snapshot.getDocuments()) {
            int rating = intValue(player, RATING, 1000);
            if (rating != previousRating) rank = results.size() + 1;
            previousRating = rating;
            results.add(new LeaderboardEntry(rank, string(player, USER_ID),
                    string(player, DISPLAY_NAME), rating, intValue(player, WINS, 0),
                    intValue(player, LOSSES, 0), intValue(player, GAMES, 0)));
        }
        return results;
    }

    private Recovery readRecovery(Transaction tx, Set<String> staleRuns) throws Exception {
        Recovery recovery = new Recovery();
        for (String runId : staleRuns) {
            DocumentReference runRef = runs.document(runId);
            recovery.runs.put(runRef, await(tx.get(runRef)));
            QuerySnapshot active = await(tx.get(matches.whereEqualTo(SERVER_RUN_ID, runId)
                    .whereEqualTo(STATUS, MATCH_RUNNING)));
            recovery.games.addAll(active.getDocuments());
        }
        if (recovery.games.size() > MAX_RECOVERY_MATCHES) throw invalid("RECOVERY_LIMIT_EXCEEDED");
        for (QueryDocumentSnapshot game : recovery.games) {
            recovery.players.add(player(string(game, FIRST_USER_ID)));
            recovery.players.add(player(string(game, SECOND_USER_ID)));
        }
        for (DocumentReference player : recovery.players)
            recovery.playerSnapshots.put(player, await(tx.get(player)));
        return recovery;
    }

    private void writeRecovery(Transaction tx, Recovery recovery, long now) {
        for (Map.Entry<DocumentReference, DocumentSnapshot> run : recovery.runs.entrySet()) {
            Map<String, Object> data = run.getValue().exists() ? copy(run.getValue())
                    : runFields(STATE_STOPPED, now, now);
            data.put(STATE, STATE_STOPPED);
            if (data.get(STOPPED_AT_MILLIS) == null) data.put(STOPPED_AT_MILLIS, now);
            tx.set(run.getKey(), data);
        }
        Map<DocumentReference, Map<String, Object>> updated = new LinkedHashMap<>();
        for (QueryDocumentSnapshot game : recovery.games) {
            Map<String, Object> data = copy(game);
            data.put(STATUS, MATCH_VOID);
            data.put(REASON, "SERVER_STOPPED");
            data.put(ENDED_AT_MILLIS, now);
            tx.set(game.getReference(), data);
            for (String uid : new String[] {string(game, FIRST_USER_ID), string(game, SECOND_USER_ID)}) {
                DocumentReference ref = player(uid);
                DocumentSnapshot snapshot = recovery.playerSnapshots.get(ref);
                if (!game.getId().equals(string(snapshot, ACTIVE_MATCH_ID)))
                    throw invalid(ACTIVE_MATCH_CONFLICT);
                Map<String, Object> stats = copy(snapshot);
                stats.put(ACTIVE_MATCH_ID, null);
                updated.put(ref, stats);
            }
        }
        for (Map.Entry<DocumentReference, Map<String, Object>> player : updated.entrySet())
            tx.set(player.getKey(), player.getValue());
    }

    private static final class Recovery {
        final Map<DocumentReference, DocumentSnapshot> runs = new LinkedHashMap<>();
        final List<QueryDocumentSnapshot> games = new ArrayList<>();
        final Set<DocumentReference> players = new LinkedHashSet<>();
        final Map<DocumentReference, DocumentSnapshot> playerSnapshots = new HashMap<>();
    }

    private void assertLease(DocumentSnapshot gate, String runId, boolean requireOpen, long now) {
        if (!runId.equals(string(gate, OWNER_RUN_ID))
                || number(gate, LEASE_UNTIL_MILLIS, 0) <= now
                || (requireOpen && !bool(gate, ENABLED))) throw invalid(RUN_NOT_OWNER);
    }

    private DocumentReference player(String userId) {
        String safe = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(userId.getBytes(StandardCharsets.UTF_8));
        return players.document(safe);
    }

    private String profileName(String uid) throws IOException {
        if (auth == null) return "Player " + uid.substring(0, Math.min(8, uid.length()));
        try {
            UserRecord user = auth.getUser(uid);
            String name = UsernameIdentity.fromEmail(user.getEmail());
            if (name == null) name = user.getDisplayName();
            if (name == null || name.trim().isEmpty() || name.length() > 32)
                name = "Player " + uid.substring(0, Math.min(8, uid.length()));
            return name;
        } catch (FirebaseAuthException e) { throw new IOException("UNKNOWN_ACCOUNT", e); }
    }

    private static Map<String, Object> playerData(DocumentSnapshot existing, String uid, String name) {
        if (existing.exists()) return copy(existing);
        Map<String, Object> result = new HashMap<>();
        result.put(USER_ID, uid);
        result.put(DISPLAY_NAME, name);
        result.put(RATING, 1000);
        result.put(WINS, 0);
        result.put(LOSSES, 0);
        result.put(GAMES, 0);
        result.put(RANKED, false);
        result.put(ACTIVE_MATCH_ID, null);
        return result;
    }

    private static Map<String, Object> settledPlayer(DocumentSnapshot existing, int rating, boolean won) {
        Map<String, Object> player = copy(existing);
        player.put(RATING, rating);
        player.put(WINS, intValue(existing, WINS, 0) + (won ? 1 : 0));
        player.put(LOSSES, intValue(existing, LOSSES, 0) + (won ? 0 : 1));
        player.put(GAMES, intValue(existing, GAMES, 0) + 1);
        player.put(RANKED, true);
        player.put(ACTIVE_MATCH_ID, null);
        return player;
    }

    /** PostgreSQL numeric round() for a positive Elo delta is equivalent to this calculation. */
    static int eloDelta(int winnerRating, int loserRating) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (loserRating - winnerRating) / 400.0));
        return (int) Math.floor(32.0 * (1.0 - expected) + 0.5);
    }

    private static MatchRecord record(DocumentSnapshot snapshot) { return record(snapshot.getData()); }
    private static MatchRecord record(Map<String, Object> data) {
        return new MatchRecord((String) data.get(MATCH_ID), (String) data.get(SERVER_RUN_ID),
                (String) data.get(FIRST_USER_ID), (String) data.get(SECOND_USER_ID),
                MatchRecord.Status.valueOf((String) data.get(STATUS)),
                (String) data.get(WINNER_USER_ID), (String) data.get(REASON),
                nullableInt(data.get(FIRST_RATING_BEFORE)), nullableInt(data.get(FIRST_RATING_AFTER)),
                nullableInt(data.get(SECOND_RATING_BEFORE)), nullableInt(data.get(SECOND_RATING_AFTER)));
    }

    private static Integer nullableInt(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static Map<String, Object> copy(DocumentSnapshot snapshot) {
        return new HashMap<>(snapshot.getData());
    }

    private static Map<String, Object> controlFields(String owner, long until, boolean enabled) {
        Map<String, Object> result = new HashMap<>();
        result.put(OWNER_RUN_ID, owner);
        result.put(LEASE_UNTIL_MILLIS, until);
        result.put(ENABLED, enabled);
        return result;
    }

    private static Map<String, Object> runFields(String state, long created, Long stopped) {
        Map<String, Object> result = new HashMap<>();
        result.put(STATE, state);
        result.put(CREATED_AT_MILLIS, created);
        result.put(STOPPED_AT_MILLIS, stopped);
        return result;
    }

    private static String string(DocumentSnapshot doc, String field) {
        return doc.exists() ? doc.getString(field) : null;
    }

    private static boolean bool(DocumentSnapshot doc, String field) {
        return doc.exists() && Boolean.TRUE.equals(doc.getBoolean(field));
    }

    private static long number(DocumentSnapshot doc, String field, long fallback) {
        Long value = doc.exists() ? doc.getLong(field) : null;
        return value == null ? fallback : value;
    }

    private static int intValue(DocumentSnapshot doc, String field, int fallback) {
        Long value = doc.exists() ? doc.getLong(field) : null;
        return value == null ? fallback : value.intValue();
    }

    private static void required(String value, String error) throws IOException {
        if (value == null || value.isEmpty() || value.length() > 256 || value.indexOf('/') >= 0)
            throw new IOException(error);
    }

    private static void bounded(String value, String error) throws IOException {
        if (value == null || value.isEmpty() || value.length() > 100) throw new IOException(error);
    }

    private static IllegalStateException invalid(String code) { return new IllegalStateException(code); }

    private interface Work<T> { T apply(Transaction tx) throws Exception; }

    private <T> T transaction(Work<T> work) throws IOException {
        return await(firestore.runTransaction(work::apply));
    }

    private static <T> T await(ApiFuture<T> future) throws IOException {
        try { return future.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Interrupted", e); }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause != null && cause.getCause() != null
                    && !(cause instanceof IllegalStateException)) cause = cause.getCause();
            if (cause instanceof IllegalStateException) throw new IOException(cause.getMessage(), cause);
            throw new IOException("Firestore operation failed", e.getCause());
        }
    }
}
