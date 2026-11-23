package kr.ac.jbnu.se.tetris.firebase.server;

/**
 * Firestore 공식 대전 저장소의 컬렉션·필드·상태 이름.
 * 저장소, 신원 등록, 이관 도구가 같은 문서를 읽고 쓰므로 이름을 한곳에서 정의한다.
 * 값은 이미 저장된 운영 데이터와 보안 규칙에 묶여 있으므로 바꾸면 안 된다.
 */
public final class FirestoreSchema {
    // 컬렉션
    public static final String RANKED_CONTROL = "ranked_control";
    public static final String RANKED_RUNS = "ranked_runs";
    public static final String RANKED_MATCHES = "ranked_matches";
    public static final String RANKED_PLAYERS = "ranked_players";
    public static final String RANKED_IDENTITIES = "ranked_identities";
    public static final String RANKED_USERNAMES = "ranked_usernames";
    public static final String MIGRATION_STATE = "migration_state";

    // 고정 문서 ID
    public static final String CONTROL_DOCUMENT = "singleton";
    public static final String MIGRATION_DOCUMENT = "supabase_v1";

    // 접수 제어·서버 실행 필드
    public static final String OWNER_RUN_ID = "ownerRunId";
    public static final String LEASE_UNTIL_MILLIS = "leaseUntilMillis";
    public static final String ENABLED = "enabled";
    public static final String STATE = "state";
    public static final String CREATED_AT_MILLIS = "createdAtMillis";
    public static final String STOPPED_AT_MILLIS = "stoppedAtMillis";
    public static final String MIGRATION_DIGEST = "migrationDigest";

    // 경기 필드
    public static final String MATCH_ID = "matchId";
    public static final String SERVER_RUN_ID = "serverRunId";
    public static final String RULES_VERSION = "rulesVersion";
    public static final String FIRST_USER_ID = "firstUserId";
    public static final String SECOND_USER_ID = "secondUserId";
    public static final String STATUS = "status";
    public static final String WINNER_USER_ID = "winnerUserId";
    public static final String REASON = "reason";
    public static final String STARTED_AT_MILLIS = "startedAtMillis";
    public static final String ENDED_AT_MILLIS = "endedAtMillis";
    public static final String FIRST_RATING_BEFORE = "firstRatingBefore";
    public static final String FIRST_RATING_AFTER = "firstRatingAfter";
    public static final String SECOND_RATING_BEFORE = "secondRatingBefore";
    public static final String SECOND_RATING_AFTER = "secondRatingAfter";

    // 선수·신원 필드
    public static final String USER_ID = "userId";
    public static final String DISPLAY_NAME = "displayName";
    public static final String EMAIL = "email";
    public static final String RATING = "rating";
    public static final String WINS = "wins";
    public static final String LOSSES = "losses";
    public static final String GAMES = "games";
    public static final String RANKED = "ranked";
    public static final String ACTIVE_MATCH_ID = "activeMatchId";

    // 상태 값
    public static final String STATE_ACTIVE = "ACTIVE";
    public static final String STATE_STOPPED = "STOPPED";
    public static final String MATCH_RUNNING = "RUNNING";
    public static final String MATCH_FINALIZED = "FINALIZED";
    public static final String MATCH_VOID = "VOID";
    public static final String MARKER_COMPLETE = "COMPLETE";
    public static final String MARKER_IN_PROGRESS = "IN_PROGRESS";

    private FirestoreSchema() { }
}
