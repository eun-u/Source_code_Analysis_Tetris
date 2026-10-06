package kr.ac.jbnu.se.tetris.auth;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;
import kr.ac.jbnu.se.tetris.supabase.SupabaseHttp;
import kr.ac.jbnu.se.tetris.supabase.SupabaseHttpException;

/** 공개 키로 Supabase Auth를 호출한다. 토큰은 이 객체의 메모리에만 둔다. */
public final class SupabaseAuthService {
    private final SupabaseHttp http;
    private AuthSession session;

    public SupabaseAuthService(SupabaseConfig config) { http = new SupabaseHttp(config); }

    public synchronized SignUpResult signUp(String email, String password) throws AuthException, IOException {
        session = null;
        Map<String, Object> result = request("POST", "/auth/v1/signup", credentials(email, password), null);
        Map<String, Object> user = object(result.get("user"), false);
        if (user == null) user = result;
        String id = string(user.get("id"));
        if (id == null) throw new IOException("Invalid signup response");
        if (string(result.get("access_token")) != null) {
            session = parseSession(result);
            return new SignUpResult(id, false, session);
        }
        return new SignUpResult(id, true, null);
    }

    public synchronized AuthSession signIn(String email, String password) throws AuthException, IOException {
        session = null;
        session = parseSession(request("POST", "/auth/v1/token?grant_type=password",
                credentials(email, password), null));
        return session;
    }

    public synchronized AuthSession refresh() throws AuthException, IOException {
        AuthSession old = requireSession();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("refresh_token", old.getRefreshToken());
        try {
            AuthSession updated = parseSession(request("POST", "/auth/v1/token?grant_type=refresh_token", body, null));
            if (!old.getUserId().equals(updated.getUserId())) {
                session = null;
                throw new AuthException("IDENTITY_MISMATCH");
            }
            session = updated;
            return updated;
        } catch (AuthException error) {
            String code = error.getCode().toLowerCase(java.util.Locale.ROOT);
            if (error.getHttpStatus() == 401 || error.getHttpStatus() == 403
                    || "invalid_credentials".equals(code) || "invalid_grant".equals(code)
                    || code.startsWith("refresh_token_") || "session_not_found".equals(code)) session = null;
            throw error;
        }
    }

    public synchronized void signOut() throws AuthException, IOException {
        AuthSession current = session;
        session = null;
        if (current != null) request("POST", "/auth/v1/logout?scope=local",
                new LinkedHashMap<String, Object>(), current.getAccessToken());
    }

    public void recoverPassword(String email) throws AuthException, IOException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("email", required(email, "email"));
        request("POST", "/auth/v1/recover", body, null);
    }

    /** 원격 메일 템플릿에 {{ .Token }} 일회용 코드가 있어야 사용할 수 있다. */
    public synchronized AuthSession completePasswordRecovery(String email, String otp, String password)
            throws AuthException, IOException {
        session = null;
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("type", "recovery");
        body.put("email", required(email, "email"));
        body.put("token", required(otp, "otp"));
        AuthSession recovered = parseSession(request("POST", "/auth/v1/verify", body, null));
        Map<String, Object> change = new LinkedHashMap<String, Object>();
        change.put("password", requiredPassword(password));
        request("PUT", "/auth/v1/user", change, recovered.getAccessToken());
        session = recovered;
        return recovered;
    }

    public synchronized AuthSession changePassword(String password) throws AuthException, IOException {
        AuthSession current = requireSession();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("password", requiredPassword(password));
        request("PUT", "/auth/v1/user", body, current.getAccessToken());
        return current;
    }

    public synchronized AuthSession getSession() { return session; }

    private AuthSession requireSession() throws AuthException {
        if (session == null) throw new AuthException("NOT_SIGNED_IN");
        return session;
    }

    private Map<String, Object> request(String method, String path, Object body, String bearer)
            throws IOException, AuthException {
        try { return object(http.request(method, path, body, bearer), true); }
        catch (SupabaseHttpException error) {
            int status = error.getStatus();
            if (status == 400 || status == 401 || status == 403 || status == 422 || status == 429)
                throw new AuthException(error.getCode(), status);
            throw error;
        }
    }

    private static Map<String, Object> credentials(String email, String password) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("email", required(email, "email"));
        result.put("password", requiredPassword(password));
        return result;
    }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty() || value.length() > 4096)
            throw new IllegalArgumentException(field + " required");
        return value.trim();
    }

    private static String requiredPassword(String value) {
        if (value == null || value.isEmpty() || value.length() > 4096)
            throw new IllegalArgumentException("password required");
        return value;
    }

    private static AuthSession parseSession(Map<String, Object> body) throws IOException {
        String access = string(body.get("access_token"));
        String refresh = string(body.get("refresh_token"));
        Map<String, Object> user = object(body.get("user"), false);
        String id = user == null ? null : string(user.get("id"));
        long expiry = number(body.get("expires_at"));
        if (expiry <= 0) expiry = Instant.now().getEpochSecond() + number(body.get("expires_in"));
        if (expiry <= Instant.now().getEpochSecond()) throw new IOException("Expired Auth session response");
        try { return new AuthSession(access, refresh, id, expiry); }
        catch (IllegalArgumentException error) { throw new IOException("Invalid Auth session response", error); }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, boolean required) throws IOException {
        if (value instanceof Map) return (Map<String, Object>) value;
        if (required) throw new IOException("Invalid Auth response");
        return null;
    }

    private static String string(Object value) { return value instanceof String ? (String) value : null; }
    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
}
