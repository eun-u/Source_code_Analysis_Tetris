package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Properties;
import kr.ac.jbnu.se.tetris.auth.AuthService;
import kr.ac.jbnu.se.tetris.auth.FirebaseAuthService;
import kr.ac.jbnu.se.tetris.auth.SupabaseAuthService;
import kr.ac.jbnu.se.tetris.firebase.FirebaseConfig;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardService;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;

/** Public online endpoints and publishable Auth keys. Never stores account tokens. */
public final class RankedOnlineConfig {
    private static final String PROVIDER_FIREBASE = "firebase";
    private static final String PROVIDER_SUPABASE = "supabase";
    private final URI serverUri;
    private final SupabaseConfig supabaseConfig;
    private final FirebaseConfig firebaseConfig;

    public RankedOnlineConfig(URI serverUri, SupabaseConfig supabaseConfig) {
        if (serverUri == null || supabaseConfig == null) throw new IllegalArgumentException("Online configuration required");
        new ConnectionOptions(serverUri, "configuration-validation");
        this.serverUri = serverUri;
        this.supabaseConfig = supabaseConfig;
        this.firebaseConfig = null;
    }

    public RankedOnlineConfig(URI serverUri, FirebaseConfig firebaseConfig) {
        if (serverUri == null || firebaseConfig == null) throw new IllegalArgumentException("Online configuration required");
        new ConnectionOptions(serverUri, "configuration-validation");
        this.serverUri = serverUri;
        this.supabaseConfig = null;
        this.firebaseConfig = firebaseConfig;
    }

    public URI getServerUri() { return serverUri; }
    public SupabaseConfig getSupabaseConfig() { return supabaseConfig; }
    public FirebaseConfig getFirebaseConfig() { return firebaseConfig; }
    public boolean isFirebase() { return firebaseConfig != null; }
    public AuthService createAuthService() {
        return isFirebase() ? new FirebaseAuthService(firebaseConfig) : new SupabaseAuthService(supabaseConfig);
    }
    public LeaderboardService createLeaderboardService() {
        return isFirebase() ? new LeaderboardService(serverUri) : new LeaderboardService(supabaseConfig);
    }

    public static RankedOnlineConfig load() throws IOException {
        Properties properties = readProperties();
        String server = value(properties, "server.url", "TETRIS_SERVER_URL");
        String provider = value(properties, "online.provider", "TETRIS_ONLINE_PROVIDER");
        String firebaseProject = value(properties, "firebase.projectId", "FIREBASE_PROJECT_ID");
        String firebaseKey = value(properties, "firebase.apiKey", "FIREBASE_API_KEY");
        String url = value(properties, "supabase.url", "SUPABASE_URL");
        String key = value(properties, "supabase.publishableKey", "SUPABASE_PUBLISHABLE_KEY");
        if (server.isEmpty() && provider.isEmpty() && firebaseProject.isEmpty()
                && firebaseKey.isEmpty() && url.isEmpty() && key.isEmpty()) return null;
        if (provider.isEmpty()) {
            provider = !firebaseProject.isEmpty() || !firebaseKey.isEmpty() ? PROVIDER_FIREBASE : PROVIDER_SUPABASE;
        }
        if (!PROVIDER_FIREBASE.equalsIgnoreCase(provider) && !PROVIDER_SUPABASE.equalsIgnoreCase(provider))
            throw new IOException("Unknown online provider");
        try {
            if (PROVIDER_FIREBASE.equalsIgnoreCase(provider)) {
                return new RankedOnlineConfig(URI.create(requireServer(server, firebaseProject, firebaseKey,
                        "Incomplete public Firebase online configuration")),
                        firebaseConfig(properties, firebaseProject, firebaseKey));
            }
            requireServer(server, url, key, "Incomplete public Supabase online configuration");
            return new RankedOnlineConfig(URI.create(server), new SupabaseConfig(url, key));
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Invalid public online configuration", error);
        }
    }

    /** 외부 설정 파일이 있으면 그것만 읽고, 없으면 JAR에 포함된 공개 기본값을 읽는다. */
    private static Properties readProperties() throws IOException {
        Properties properties = new Properties();
        Path file = configFile();
        if (file != null && Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } else {
            // 공개 접속 정보만 기본값으로 배포해 새 PC의 dev 빌드도 온라인 서버를 찾는다.
            // 외부 파일·환경 변수·시스템 속성은 기존처럼 우선하며 비밀 키/토큰은 포함하지 않는다.
            try (InputStream defaults = RankedOnlineConfig.class.getResourceAsStream("/online-defaults.properties")) {
                if (defaults != null) properties.load(defaults);
            }
        }
        return properties;
    }

    private static String requireServer(String server, String first, String second, String message)
            throws IOException {
        if (server.isEmpty() || first.isEmpty() || second.isEmpty()) throw new IOException(message);
        return server;
    }

    /** 두 에뮬레이터 주소가 모두 있을 때만 로컬 Firebase 에뮬레이터로 연결한다. */
    private static FirebaseConfig firebaseConfig(Properties properties, String project, String apiKey)
            throws IOException {
        String authEmulator = value(properties, "firebase.authEmulatorUrl", "FIREBASE_AUTH_EMULATOR_URL");
        String tokenEmulator = value(properties, "firebase.tokenEmulatorUrl", "FIREBASE_TOKEN_EMULATOR_URL");
        FirebaseConfig firebase = new FirebaseConfig(project, apiKey);
        if (authEmulator.isEmpty() && tokenEmulator.isEmpty()) return firebase;
        if (authEmulator.isEmpty() || tokenEmulator.isEmpty())
            throw new IOException("Both local Firebase emulator URLs are required");
        return FirebaseConfig.forLocalEmulator(project, apiKey, URI.create(authEmulator), URI.create(tokenEmulator));
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
