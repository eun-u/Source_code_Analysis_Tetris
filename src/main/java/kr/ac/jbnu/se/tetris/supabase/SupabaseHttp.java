package kr.ac.jbnu.se.tetris.supabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import kr.ac.jbnu.se.tetris.network.protocol.StrictJson;

/** 크기와 시간을 제한한 인증/랭킹 HTTP 클라이언트. Swing EDT 밖에서 호출한다. */
public final class SupabaseHttp {
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private final SupabaseConfig config;

    public SupabaseHttp(SupabaseConfig config) {
        if (config == null) throw new IllegalArgumentException("Supabase configuration required");
        this.config = config;
    }

    public Object request(String method, String path, Object payload, String bearer) throws IOException {
        if (!("POST".equals(method) || "PUT".equals(method)) || path == null
                || !path.startsWith("/") || path.startsWith("//") || path.contains("..")
                || path.contains("#")) throw new IllegalArgumentException("Invalid Supabase request");
        HttpURLConnection connection = (HttpURLConnection) config.getBaseUri().resolve(path).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("apikey", config.getPublishableKey());
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
        try {
            if (payload != null) {
                byte[] body = StrictJson.stringify(payload);
                if (body.length > 65536) throw new IllegalArgumentException("Supabase request too large");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) { output.write(body); }
            }
            int status = connection.getResponseCode();
            InputStream response = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] bytes = response == null ? new byte[0] : readBounded(response);
            Object parsed = bytes.length == 0 ? Collections.emptyMap() : StrictJson.parse(bytes);
            if (status < 200 || status >= 300) {
                String code = "HTTP_" + status;
                if (parsed instanceof Map) {
                    Object provided = ((Map<?, ?>) parsed).get("code");
                    if (!(provided instanceof String)) provided = ((Map<?, ?>) parsed).get("error_code");
                    if (provided instanceof String && !((String) provided).isEmpty()) code = (String) provided;
                }
                throw new SupabaseHttpException(status, code);
            }
            return parsed;
        } finally { connection.disconnect(); }
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (count + output.size() > MAX_RESPONSE_BYTES) throw new IOException("Supabase response too large");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
}
