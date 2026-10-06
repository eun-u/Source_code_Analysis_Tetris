package kr.ac.jbnu.se.tetris.ranking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;
import kr.ac.jbnu.se.tetris.supabase.SupabaseHttp;

/** 서버 확정 온라인 PvP 전적만 읽는 공개 랭킹 클라이언트. */
public final class LeaderboardService {
    private final SupabaseHttp http;

    public LeaderboardService(SupabaseConfig config) { http = new SupabaseHttp(config); }

    public List<LeaderboardEntry> top100(String accessToken) throws IOException {
        if (accessToken == null || accessToken.isEmpty()) throw new IllegalArgumentException("accessToken");
        Object result = http.request("POST", "/rest/v1/rpc/ranked_leaderboard",
                Collections.emptyMap(), accessToken);
        if (!(result instanceof List)) throw new IOException("Invalid leaderboard response");
        List<?> rows = (List<?>) result;
        if (rows.size() > 100) throw new IOException("Leaderboard limit exceeded");
        List<LeaderboardEntry> entries = new ArrayList<LeaderboardEntry>();
        try {
            for (Object raw : rows) {
                Map<?, ?> row = (Map<?, ?>) raw;
                entries.add(new LeaderboardEntry(integer(row, "rank"), string(row, "user_id"),
                        string(row, "display_name"), integer(row, "rating"), integer(row, "wins"),
                        integer(row, "losses"), integer(row, "games")));
            }
        } catch (RuntimeException error) { throw new IOException("Invalid leaderboard response", error); }
        return Collections.unmodifiableList(entries);
    }

    private static String string(Map<?, ?> row, String key) {
        Object value = row.get(key);
        if (!(value instanceof String)) throw new IllegalArgumentException("Invalid leaderboard field");
        return (String) value;
    }

    private static int integer(Map<?, ?> row, String key) {
        Object value = row.get(key);
        if (!(value instanceof Number)) throw new IllegalArgumentException("Invalid leaderboard field");
        return ((Number) value).intValue();
    }
}
