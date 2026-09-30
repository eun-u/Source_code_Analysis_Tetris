package kr.ac.jbnu.se.tetris.ranking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;
import kr.ac.jbnu.se.tetris.supabase.SupabaseHttp;

/** Authenticated read-only leaderboard access from the desktop client. */
public final class LeaderboardService {
    private final SupabaseHttp http;

    public LeaderboardService(SupabaseConfig config) { http = new SupabaseHttp(config); }

    public List<LeaderboardEntry> top100(String accessToken) throws IOException {
        if (accessToken == null || accessToken.isEmpty()) throw new IllegalArgumentException("accessToken");
        JsonElement response = http.request("POST", "/rest/v1/rpc/ranked_leaderboard",
                new JsonObject(), accessToken, false);
        try {
            JsonArray array = response.getAsJsonArray();
            List<LeaderboardEntry> entries = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject row = element.getAsJsonObject();
                entries.add(new LeaderboardEntry(row.get("rank").getAsInt(), row.get("user_id").getAsString(),
                        row.get("display_name").getAsString(), row.get("rating").getAsInt(),
                        row.get("wins").getAsInt(), row.get("losses").getAsInt(),
                        row.get("games").getAsInt()));
            }
            if (entries.size() > 100) throw new IOException("Leaderboard limit exceeded");
            return Collections.unmodifiableList(entries);
        } catch (RuntimeException e) { throw new IOException("Invalid leaderboard response", e); }
    }
}
