package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Properties;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;

/** 공개 서버 주소와 공개 Supabase 키만 읽는다. 계정 토큰은 설정 파일에 기록하지 않는다. */
public final class RankedOnlineConfig {
    private final URI serverUri;
    private final SupabaseConfig supabaseConfig;

    public RankedOnlineConfig(URI serverUri, SupabaseConfig supabaseConfig) {
        if (serverUri == null || supabaseConfig == null) throw new IllegalArgumentException("Online configuration required");
        new ConnectionOptions(serverUri, "configuration-validation");
        this.serverUri = serverUri;
        this.supabaseConfig = supabaseConfig;
    }

    public URI getServerUri() { return serverUri; }
    public SupabaseConfig getSupabaseConfig() { return supabaseConfig; }

    public static RankedOnlineConfig load() throws IOException {
        Properties properties = new Properties();
        Path file = configFile();
        if (file != null && Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        String server = value(properties, "server.url", "TETRIS_SERVER_URL");
        String url = value(properties, "supabase.url", "SUPABASE_URL");
        String key = value(properties, "supabase.publishableKey", "SUPABASE_PUBLISHABLE_KEY");
        if (server.isEmpty() && url.isEmpty() && key.isEmpty()) return null;
        if (server.isEmpty() || url.isEmpty() || key.isEmpty()) throw new IOException("Incomplete public online configuration");
        try {
            return new RankedOnlineConfig(URI.create(server), new SupabaseConfig(url, key));
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Invalid public online configuration", error);
        }
    }

    private static Path configFile() {
        String override = System.getProperty("tetris.client.config");
        if (override != null && !override.trim().isEmpty()) return Paths.get(override.trim());
        Path cwd = Paths.get("tetris-client.properties");
        if (Files.isRegularFile(cwd)) return cwd;
        try {
            CodeSource source = RankedOnlineConfig.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Path code = Paths.get(source.getLocation().toURI()).toAbsolutePath();
                Path sibling = code.getParent().resolve("tetris-client.properties");
                if (Files.isRegularFile(sibling)) return sibling;
            }
        } catch (Exception ignored) { /* 설정이 없으면 로컬 게임으로 진입한다. */ }
        return cwd;
    }

    private static String value(Properties properties, String name, String environment) {
        String found = System.getProperty("tetris." + name);
        if (found == null || found.trim().isEmpty()) found = System.getenv(environment);
        if (found == null || found.trim().isEmpty()) found = properties.getProperty(name, "");
        return found.trim();
    }
}
