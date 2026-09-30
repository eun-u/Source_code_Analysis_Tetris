package kr.ac.jbnu.se.tetris.app;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;

/** 서버 주소와 공개 프로젝트 키만 읽는 데스크톱 설정. 비밀키는 읽거나 저장하지 않음. */
public final class OnlineClientConfig {
    private final URI serverUri;
    private final SupabaseConfig supabase;

    public OnlineClientConfig(URI serverUri, SupabaseConfig supabase) {
        if (serverUri == null || supabase == null) throw new IllegalArgumentException("Online config required");
        new ConnectionOptions(serverUri, "configuration-validation");
        this.serverUri = serverUri; this.supabase = supabase;
    }
    public URI getServerUri() { return serverUri; }
    public SupabaseConfig getSupabase() { return supabase; }

    public static OnlineClientConfig load() throws IOException {
        Properties values = new Properties();
        Path path = Paths.get(System.getProperty("tetris.client.config", "tetris-client.properties"));
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
        }
        String server = value(values, "server.url", "TETRIS_SERVER_URL");
        String url = value(values, "supabase.url", "SUPABASE_URL");
        String key = value(values, "supabase.publishableKey", "SUPABASE_PUBLISHABLE_KEY");
        if (server.isEmpty() && url.isEmpty() && key.isEmpty()) return null;
        return new OnlineClientConfig(URI.create(server), new SupabaseConfig(url, key));
    }
    private static String value(Properties values, String key, String env) {
        String value = System.getProperty("tetris." + key);
        if (value == null || value.trim().isEmpty()) value = System.getenv(env);
        if (value == null || value.trim().isEmpty()) value = values.getProperty(key, "");
        return value.trim();
    }
}
