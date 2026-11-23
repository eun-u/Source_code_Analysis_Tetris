package kr.ac.jbnu.se.tetris.migration;

import static kr.ac.jbnu.se.tetris.firebase.server.FirestoreSchema.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.jbnu.se.tetris.auth.UsernameIdentity;

/** Validated immutable mapping of the read-only Supabase export to Firestore documents. */
public final class FirebaseMigrationPlan {
    private static final Pattern UUID = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    // Supabase 내보내기 열 이름
    private static final String COL_USER_ID = "user_id";
    private static final String COL_OUTCOME = "outcome";
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}");
    private final String sha256;
    private final long exportedAtMillis;
    private final List<User> users;
    private final Map<String, Map<String, Object>> players;
    private final Map<String, Map<String, Object>> identities;
    private final Map<String, Map<String, Object>> usernames;
    private final Map<String, Map<String, Object>> matches;
    private final Map<String, Map<String, Object>> runs;

    public static final class User {
        private final String uid;
        private final String email;
        private final String displayName;
        private final String bcryptHash;
        private final boolean emailVerified;
        private final boolean disabled;
        private User(String uid, String email, String displayName, String bcryptHash,
                     boolean emailVerified, boolean disabled) {
            this.uid = uid; this.email = email; this.displayName = displayName;
            this.bcryptHash = bcryptHash; this.emailVerified = emailVerified; this.disabled = disabled;
        }
        public String getUid() { return uid; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getBcryptHash() { return bcryptHash; }
        public boolean isEmailVerified() { return emailVerified; }
        public boolean isDisabled() { return disabled; }
    }

    private FirebaseMigrationPlan(String sha256, long exportedAtMillis, List<User> users,
                                  Map<String, Map<String, Object>> players,
                                  Map<String, Map<String, Object>> identities,
                                  Map<String, Map<String, Object>> usernames,
                                  Map<String, Map<String, Object>> matches,
                                  Map<String, Map<String, Object>> runs) {
        this.sha256 = sha256;
        this.exportedAtMillis = exportedAtMillis;
        this.users = Collections.unmodifiableList(users);
        this.players = Collections.unmodifiableMap(players);
        this.identities = Collections.unmodifiableMap(identities);
        this.usernames = Collections.unmodifiableMap(usernames);
        this.matches = Collections.unmodifiableMap(matches);
        this.runs = Collections.unmodifiableMap(runs);
    }

    public String getSha256() { return sha256; }
    public long getExportedAtMillis() { return exportedAtMillis; }
    public List<User> getUsers() { return users; }
    public Map<String, Map<String, Object>> getPlayers() { return players; }
    public Map<String, Map<String, Object>> getIdentities() { return identities; }
    public Map<String, Map<String, Object>> getUsernames() { return usernames; }
    public Map<String, Map<String, Object>> getMatches() { return matches; }
    public Map<String, Map<String, Object>> getRuns() { return runs; }

    public static FirebaseMigrationPlan load(Path path) throws IOException {
        long size = Files.size(path);
        if (size <= 0 || size > 50L * 1024 * 1024) throw new IOException("Export size is invalid");
        byte[] bytes = Files.readAllBytes(path);
        String digest = sha256(bytes);
        JsonObject root;
        try { root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (RuntimeException invalid) { throw new IOException("Invalid export JSON", invalid); }
        if (integer(root, "schemaVersion") != 1) throw new IOException("Unsupported export schema");
        long exported = timestamp(root, "exportedAt");
        if (exported > System.currentTimeMillis() + 60_000) throw new IOException("Export time is in the future");
        Loader loader = new Loader(digest, exported);
        // 섹션 사이의 참조 검증이 앞 섹션 결과를 쓰므로 읽는 순서를 바꾸면 안 된다.
        loader.readUsers(array(root, "users"));
        loader.readProfiles(array(root, "profiles"));
        loader.readPlayerStats(array(root, "player_stats"));
        loader.buildIdentities();
        loader.readRuns(array(root, "server_runs"));
        loader.readParticipants(array(root, "match_participants"));
        loader.readMatches(array(root, "matches"));
        loader.verifyPlayerTotals();
        return new FirebaseMigrationPlan(digest, exported, loader.users, loader.players, loader.identities,
                loader.usernames, loader.matches, loader.runs);
    }

    /** 내보내기 한 개를 검증하며 Firestore 문서로 옮기는 중간 상태. */
    private static final class Loader {
        private final String digest;
        private final long exported;
        private final List<User> users = new ArrayList<>();
        private final Map<String, User> byUid = new LinkedHashMap<>();
        private final Map<String, String> names = new HashMap<>();
        private final Map<String, Map<String, Object>> players = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> identities = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> usernames = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> runs = new LinkedHashMap<>();
        private final Map<String, JsonObject[]> participants = new HashMap<>();
        private final Map<String, Map<String, Object>> matches = new LinkedHashMap<>();
        private final Map<String, int[]> totals = new HashMap<>();
        private final Map<String, Long> latestTime = new HashMap<>();
        private final Map<String, Integer> latestRating = new HashMap<>();

        private Loader(String digest, long exported) {
            this.digest = digest;
            this.exported = exported;
        }

        void readUsers(JsonArray rows) throws IOException {
            Set<String> emails = new HashSet<>();
            for (JsonElement element : rows) {
                User user = readUser(object(element, "user"), emails);
                if (byUid.put(user.uid, user) != null) throw new IOException("Duplicate user ID");
                users.add(user);
            }
        }

        private User readUser(JsonObject row, Set<String> emails) throws IOException {
            String uid = id(row, "id");
            String email = required(row, "email").toLowerCase(Locale.ROOT);
            if (email.length() > 254 || email.indexOf('@') < 1 || email.indexOf(' ') >= 0
                    || !emails.add(email)) throw new IOException("Invalid or duplicate user email");
            String hash = optional(row, "encrypted_password");
            if (hash == null || !BCRYPT.matcher(hash).matches())
                throw new IOException("Unsupported password hash; bcrypt required");
            boolean confirmed = optional(row, "email_confirmed_at") != null;
            String ban = optional(row, "banned_until");
            boolean disabled = "infinity".equals(ban);
            if (ban != null && !disabled && !"-infinity".equals(ban) && parseTimestamp(ban) > exported)
                throw new IOException("Active finite ban requires an expiry-aware migration path");
            String name = UsernameIdentity.fromEmail(email);
            if (name == null) {
                JsonObject metadata = nullableObject(row, "raw_user_meta_data");
                name = metadata == null ? null : optional(metadata, "display_name");
            }
            return new User(uid, email, name, hash, confirmed, disabled);
        }

        void readProfiles(JsonArray rows) throws IOException {
            for (JsonElement element : rows) {
                JsonObject row = object(element, "profile");
                String uid = id(row, COL_USER_ID);
                String name = required(row, "display_name");
                if (!byUid.containsKey(uid) || name.length() > 32 || names.put(uid, name) != null)
                    throw new IOException("Invalid or duplicate profile");
            }
            if (!names.keySet().equals(byUid.keySet())) throw new IOException("Missing user profile");
        }

        void readPlayerStats(JsonArray rows) throws IOException {
            for (JsonElement element : rows) {
                JsonObject row = object(element, "player stats");
                String uid = id(row, COL_USER_ID);
                User user = byUid.get(uid);
                if (user == null || players.containsKey(uid)) throw new IOException("Invalid or duplicate player stats");
                int rating = integer(row, RATING);
                int wins = integer(row, WINS);
                int losses = integer(row, LOSSES);
                int games = integer(row, GAMES);
                if (wins < 0 || losses < 0 || games < 0
                        || games != (long) wins + losses || optional(row, "active_match_id") != null)
                    throw new IOException("Inconsistent player stats or active match");
                String name = UsernameIdentity.fromEmail(user.email);
                if (name == null) name = names.get(uid);
                players.put(uid, fields(USER_ID, uid, DISPLAY_NAME, name,
                        RATING, rating, WINS, wins, LOSSES, losses, GAMES, games,
                        RANKED, games > 0, ACTIVE_MATCH_ID, null, MIGRATION_DIGEST, digest));
            }
            if (!players.keySet().equals(byUid.keySet())) throw new IOException("Missing player stats");
        }

        void buildIdentities() throws IOException {
            for (User user : users) {
                String displayName = (String) players.get(user.uid).get(DISPLAY_NAME);
                identities.put(playerDocumentId(user.uid), fields(USER_ID, user.uid,
                        EMAIL, user.email, DISPLAY_NAME, displayName));
                String canonical = UsernameIdentity.fromEmail(user.email);
                if (canonical != null && usernames.put(canonical, fields(USER_ID, user.uid)) != null)
                    throw new IOException("Duplicate canonical username");
            }
        }

        /** 이관 후 이전 서버 실행은 모두 정지 상태로 기록한다. */
        void readRuns(JsonArray rows) throws IOException {
            for (JsonElement element : rows) {
                JsonObject row = object(element, "run");
                String id = id(row, "run_id");
                String state = required(row, "state");
                if (!(STATE_ACTIVE.equals(state) || STATE_STOPPED.equals(state)) || runs.containsKey(id))
                    throw new IOException("Invalid or duplicate server run");
                long created = timestamp(row, "created_at");
                Long stopped = optional(row, "stopped_at") == null ? null : timestamp(row, "stopped_at");
                if (STATE_STOPPED.equals(state) && stopped == null)
                    throw new IOException("Stopped run has no timestamp");
                runs.put(id, fields(STATE, STATE_STOPPED, CREATED_AT_MILLIS, created,
                        STOPPED_AT_MILLIS, stopped == null ? exported : stopped, MIGRATION_DIGEST, digest));
            }
        }

        void readParticipants(JsonArray rows) throws IOException {
            for (JsonElement element : rows) {
                JsonObject row = object(element, "participant");
                String matchId = id(row, "match_id");
                int slot = integer(row, "slot");
                if (slot < 1 || slot > 2) throw new IOException("Invalid participant slot");
                JsonObject[] pair = participants.computeIfAbsent(matchId, ignored -> new JsonObject[2]);
                if (pair[slot - 1] != null) throw new IOException("Duplicate participant slot");
                pair[slot - 1] = row;
            }
        }

        void readMatches(JsonArray rows) throws IOException {
            for (JsonElement element : rows) readMatch(object(element, "match"));
            if (!participants.isEmpty()) throw new IOException("Orphan match participant");
        }

        private void readMatch(JsonObject row) throws IOException {
            String matchId = id(row, "match_id");
            String runId = id(row, "server_run_id");
            String first = id(row, "first_user_id");
            String second = id(row, "second_user_id");
            String status = required(row, "status");
            String rules = required(row, "rules_version");
            if (!runs.containsKey(runId) || !byUid.containsKey(first) || !byUid.containsKey(second)
                    || first.equals(second) || rules.length() > 100 || matches.containsKey(matchId)
                    || !(MATCH_FINALIZED.equals(status) || MATCH_VOID.equals(status)))
                throw new IOException("Invalid, running, or duplicate match");
            JsonObject[] pair = participants.remove(matchId);
            if (pair == null || pair[0] == null || pair[1] == null
                    || !first.equals(id(pair[0], COL_USER_ID)) || !second.equals(id(pair[1], COL_USER_ID)))
                throw new IOException("Match participants do not match slots");
            String winner = optional(row, "winner_user_id");
            String reason = required(row, "reason");
            if (reason.length() > 100) throw new IOException("Invalid match reason");
            long started = timestamp(row, "started_at");
            long ended = timestamp(row, "ended_at");
            if (ended < started || ended > exported) throw new IOException("Invalid match timestamps");
            MatchRatings ratings = new MatchRatings(pair);
            if (MATCH_FINALIZED.equals(status)) {
                verifyFinalized(pair, first, second, winner, ratings);
                tally(totals, first, winner.equals(first));
                tally(totals, second, winner.equals(second));
                latest(latestTime, latestRating, first, ended, ratings.firstAfter);
                latest(latestTime, latestRating, second, ended, ratings.secondAfter);
            } else {
                verifyVoid(pair, winner, ratings);
            }
            matches.put(matchId, fields(MATCH_ID, matchId, SERVER_RUN_ID, runId,
                    RULES_VERSION, rules, FIRST_USER_ID, first, SECOND_USER_ID, second,
                    STATUS, status, WINNER_USER_ID, winner, REASON, reason,
                    STARTED_AT_MILLIS, started, ENDED_AT_MILLIS, ended,
                    FIRST_RATING_BEFORE, ratings.firstBefore, FIRST_RATING_AFTER, ratings.firstAfter,
                    SECOND_RATING_BEFORE, ratings.secondBefore, SECOND_RATING_AFTER, ratings.secondAfter,
                    MIGRATION_DIGEST, digest));
        }

        void verifyPlayerTotals() throws IOException {
            for (Map.Entry<String, Map<String, Object>> entry : players.entrySet()) {
                String uid = entry.getKey();
                Map<String, Object> player = entry.getValue();
                int[] score = totals.getOrDefault(uid, new int[2]);
                if (score[0] != (Integer) player.get(WINS) || score[1] != (Integer) player.get(LOSSES))
                    throw new IOException("Player totals do not match finalized matches");
                if (latestRating.containsKey(uid) && !latestRating.get(uid).equals(player.get(RATING)))
                    throw new IOException("Player rating does not match latest finalized match");
            }
        }
    }

    /** 두 참가자 행의 경기 전후 평점. 무효 경기는 네 값이 모두 비어 있어야 한다. */
    private static final class MatchRatings {
        final Integer firstBefore;
        final Integer firstAfter;
        final Integer secondBefore;
        final Integer secondAfter;

        MatchRatings(JsonObject[] pair) throws IOException {
            firstBefore = nullableInteger(pair[0], "rating_before");
            firstAfter = nullableInteger(pair[0], "rating_after");
            secondBefore = nullableInteger(pair[1], "rating_before");
            secondAfter = nullableInteger(pair[1], "rating_after");
        }

        boolean complete() {
            return firstBefore != null && firstAfter != null && secondBefore != null && secondAfter != null;
        }

        boolean empty() {
            return firstBefore == null && firstAfter == null && secondBefore == null && secondAfter == null;
        }
    }

    /** 확정 경기는 승자·평점 변화량·참가자 결과가 서버 Elo 계산과 정확히 같아야 한다. */
    private static void verifyFinalized(JsonObject[] pair, String first, String second, String winner,
            MatchRatings ratings) throws IOException {
        if (!(first.equals(winner) || second.equals(winner)) || !ratings.complete())
            throw new IOException("Inconsistent finalized match result");
        boolean firstWon = winner.equals(first);
        long firstDelta = (long) ratings.firstAfter - ratings.firstBefore;
        long secondDelta = (long) ratings.secondAfter - ratings.secondBefore;
        long winnerBefore = firstWon ? ratings.firstBefore : ratings.secondBefore;
        long loserBefore = firstWon ? ratings.secondBefore : ratings.firstBefore;
        double expectedWin = 1.0 / (1.0 + Math.pow(10.0, (loserBefore - winnerBefore) / 400.0));
        long eloDelta = (long) Math.floor(32.0 * (1.0 - expectedWin) + 0.5);
        if (firstDelta != -secondDelta
                || firstDelta != (firstWon ? eloDelta : -eloDelta)
                || !required(pair[0], COL_OUTCOME).equals(firstWon ? "WIN" : "LOSS")
                || !required(pair[1], COL_OUTCOME).equals(winner.equals(second) ? "WIN" : "LOSS"))
            throw new IOException("Inconsistent finalized match result");
    }

    private static void verifyVoid(JsonObject[] pair, String winner, MatchRatings ratings) throws IOException {
        if (winner != null || !ratings.empty()
                || !MATCH_VOID.equals(required(pair[0], COL_OUTCOME))
                || !MATCH_VOID.equals(required(pair[1], COL_OUTCOME)))
            throw new IOException("Inconsistent void match result");
    }

    private static void tally(Map<String, int[]> totals, String uid, boolean won) {
        totals.computeIfAbsent(uid, ignored -> new int[2])[won ? 0 : 1]++;
    }

    private static void latest(Map<String, Long> times, Map<String, Integer> ratings,
                               String uid, long ended, int after) {
        if (ended >= times.getOrDefault(uid, Long.MIN_VALUE)) {
            times.put(uid, ended); ratings.put(uid, after);
        }
    }

    public static String playerDocumentId(String uid) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uid.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return Collections.unmodifiableMap(result);
    }

    private static JsonArray array(JsonObject root, String name) throws IOException {
        if (!root.has(name) || !root.get(name).isJsonArray()) throw new IOException("Missing export section: " + name);
        return root.getAsJsonArray(name);
    }
    private static JsonObject object(JsonElement element, String name) throws IOException {
        if (!element.isJsonObject()) throw new IOException("Invalid export " + name);
        return element.getAsJsonObject();
    }
    private static JsonObject nullableObject(JsonObject row, String name) throws IOException {
        JsonElement element = row.get(name);
        if (element == null || element.isJsonNull()) return null;
        return object(element, name);
    }
    private static String optional(JsonObject row, String name) throws IOException {
        JsonElement element = row.get(name);
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive()) throw new IOException("Invalid export field: " + name);
        return element.getAsString();
    }
    private static String required(JsonObject row, String name) throws IOException {
        String value = optional(row, name);
        if (value == null || value.trim().isEmpty()) throw new IOException("Missing export field: " + name);
        return value;
    }
    private static String id(JsonObject row, String name) throws IOException {
        String value = required(row, name);
        if (!UUID.matcher(value).matches()) throw new IOException("Invalid UUID: " + name);
        return value;
    }
    private static int integer(JsonObject row, String name) throws IOException {
        String value = required(row, name);
        try { return Integer.parseInt(value); }
        catch (NumberFormatException invalid) { throw new IOException("Invalid integer: " + name, invalid); }
    }
    private static Integer nullableInteger(JsonObject row, String name) throws IOException {
        String value = optional(row, name);
        if (value == null) return null;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException invalid) { throw new IOException("Invalid integer: " + name, invalid); }
    }
    private static long timestamp(JsonObject row, String name) throws IOException {
        return parseTimestamp(required(row, name));
    }
    private static long parseTimestamp(String value) throws IOException {
        try { return OffsetDateTime.parse(value).toInstant().toEpochMilli(); }
        catch (RuntimeException invalid) { throw new IOException("Invalid export timestamp", invalid); }
    }
    private static String sha256(byte[] bytes) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) hex.append(String.format("%02x", value & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
    }
}
