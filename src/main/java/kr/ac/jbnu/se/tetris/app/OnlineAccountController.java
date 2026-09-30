package kr.ac.jbnu.se.tetris.app;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.auth.AuthException;
import kr.ac.jbnu.se.tetris.auth.AuthSession;
import kr.ac.jbnu.se.tetris.auth.SignUpResult;
import kr.ac.jbnu.se.tetris.auth.SupabaseAuthService;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardEntry;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardService;
import kr.ac.jbnu.se.tetris.ui.OnlineAccountPanel;
import kr.ac.jbnu.se.tetris.ui.ScreenRouter;

/** 계정 API 호출을 EDT와 분리하고 화면 종료 후 도착한 응답을 폐기하는 앱 연결부 */
public final class OnlineAccountController implements OnlineAccountPanel.Actions, AutoCloseable {
    private final OnlineClientConfig config;
    private final SupabaseAuthService auth;
    private final LeaderboardService leaderboard;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "tetris-account"); thread.setDaemon(true); return thread;
    });
    private final OnlineAccountPanel panel;
    private final Runnable beforeIdentityChange;
    private AuthSession session;
    private boolean busy;
    private boolean closed;
    private long generation;
    private Consumer<String> tokenListener = token -> { };
    private final javax.swing.Timer refreshTimer;

    public OnlineAccountController(OnlineClientConfig config, Runnable back, Runnable beforeIdentityChange) {
        ScreenRouter.requireEdt(); this.config = config; this.beforeIdentityChange = beforeIdentityChange;
        auth = config == null ? null : new SupabaseAuthService(config.getSupabase());
        leaderboard = config == null ? null : new LeaderboardService(config.getSupabase());
        panel = new OnlineAccountPanel(this, back); panel.setConfigured(config != null);
        if (config == null) panel.setMessage("온라인 서비스 연결을 준비 중입니다. 로컬 플레이는 이용할 수 있습니다.");
        refreshTimer = new javax.swing.Timer(30000, event -> refreshIfNeeded());
        refreshTimer.setCoalesce(true);
    }
    public OnlineAccountPanel getPanel() { return panel; }
    public boolean isConfigured() { return config != null; }
    public boolean isSignedIn() { ScreenRouter.requireEdt(); return session != null; }
    public void setTokenListener(Consumer<String> listener) { ScreenRouter.requireEdt(); tokenListener = listener; }

    @Override public void signIn(String email, String password) {
        if (!canSubmit()) return;
        beginIdentityChange();
        submit("로그인 중...", () -> {
            AuthSession signedIn = auth.signIn(email, password);
            return () -> adopt(signedIn, "로그인했습니다. 로비에서 온라인 대전에 접속하세요.");
        });
    }
    @Override public void signUp(String email, String password) {
        if (!canSubmit()) return;
        beginIdentityChange();
        submit("가입 요청 중...", () -> {
            SignUpResult result = auth.signUp(email, password);
            return () -> {
                if (result.getSession() != null) adopt(result.getSession(), "가입과 로그인이 완료되었습니다.");
                else panel.setMessage("메일의 인증 링크를 확인한 뒤 로그인하세요.");
            };
        });
    }
    @Override public void recover(String email) {
        submit("복구 메일 요청 중...", () -> {
            auth.recoverPassword(email);
            return () -> panel.setMessage("해당 계정이 있다면 복구 메일이 발송됩니다. 인증번호와 새 비밀번호를 입력하세요.");
        });
    }
    @Override public void completeRecovery(String email, String code, String password) {
        if (!canSubmit()) return;
        beginIdentityChange();
        submit("인증번호 확인 중...", () -> {
            AuthSession recovered = auth.completePasswordRecovery(email, code, password);
            return () -> adopt(recovered, "비밀번호를 변경하고 로그인했습니다.");
        });
    }
    @Override public void signOut() {
        if (!canSubmit()) return;
        beforeIdentityChange.run(); session = null; refreshTimer.stop(); panel.setSignedIn(false);
        submit("로그아웃 중...", () -> {
            try { auth.signOut(); }
            catch (Exception failed) { return () -> panel.setMessage("이 기기에서 로그아웃했습니다. 원격 세션 종료는 확인하지 못했습니다."); }
            return () -> panel.setMessage("로그아웃했습니다.");
        });
    }
    @Override public void refreshLeaderboard() {
        submit("랭킹을 불러오는 중...", () -> {
            AuthSession fresh = usableSession(); List<LeaderboardEntry> entries = leaderboard.top100(fresh.getAccessToken());
            return () -> { adopt(fresh, "랭킹을 갱신했습니다."); panel.setLeaderboard(entries); };
        });
    }
    public void connect(Consumer<ConnectionOptions> connected) {
        submit("온라인 접속을 준비하는 중...", () -> {
            AuthSession fresh = usableSession();
            ConnectionOptions options = new ConnectionOptions(config.getServerUri(), fresh.getAccessToken());
            return () -> { adopt(fresh, "서버에 접속하는 중..."); connected.accept(options); };
        });
    }
    private AuthSession usableSession() throws Exception {
        AuthSession current = auth.getSession();
        if (current == null) throw new AuthException("NOT_SIGNED_IN");
        if (current.getExpiresAtEpochSecond() <= Instant.now().getEpochSecond() + 120) current = auth.refresh();
        return current;
    }
    private void adopt(AuthSession value, String message) {
        session = value; panel.setSignedIn(value != null); panel.setMessage(message);
        if (value != null) refreshTimer.start(); else refreshTimer.stop();
    }
    private void beginIdentityChange() {
        beforeIdentityChange.run(); session = null; refreshTimer.stop(); panel.setSignedIn(false);
    }
    private void refreshIfNeeded() {
        if (!canSubmit() || session == null || session.getExpiresAtEpochSecond() > Instant.now().getEpochSecond() + 120) return;
        submit("로그인 상태를 갱신하는 중...", () -> {
            AuthSession refreshed = auth.refresh();
            return () -> { adopt(refreshed, "로그인 상태를 갱신했습니다."); tokenListener.accept(refreshed.getAccessToken()); };
        });
    }
    private boolean canSubmit() {
        ScreenRouter.requireEdt(); return !closed && !busy && config != null;
    }
    private void submit(String message, Callable<Runnable> task) {
        if (!canSubmit()) return;
        busy = true; panel.setBusy(true); panel.setMessage(message);
        final long attempt = generation;
        worker.execute(() -> {
            Runnable completion;
            try { completion = task.call(); }
            catch (AuthException failure) {
                AuthSession retained = auth.getSession();
                completion = () -> {
                    session = retained; panel.setSignedIn(retained != null);
                    if (retained == null) { refreshTimer.stop(); beforeIdentityChange.run(); }
                    panel.setMessage(authMessage(failure.getCode()));
                };
            } catch (IllegalArgumentException failure) {
                completion = () -> panel.setMessage("이메일·비밀번호·인증번호를 확인하세요.");
            } catch (Exception failure) {
                completion = () -> panel.setMessage("서비스에 연결하지 못했습니다. 잠시 후 다시 시도하세요.");
            }
            final Runnable result = completion;
            SwingUtilities.invokeLater(() -> {
                if (closed || attempt != generation) return;
                busy = false; panel.setBusy(false); result.run();
            });
        });
    }
    private static String authMessage(String code) {
        if ("NOT_SIGNED_IN".equals(code)) return "먼저 로그인하세요.";
        if ("EMAIL_NOT_CONFIRMED".equals(code) || "email_not_confirmed".equals(code)) return "메일 인증을 완료한 뒤 로그인하세요.";
        if ("RATE_LIMITED".equals(code) || code.toLowerCase(java.util.Locale.ROOT).contains("rate")) return "요청이 많습니다. 잠시 후 다시 시도하세요.";
        return "인증하지 못했습니다. 입력 내용과 메일 인증 여부를 확인하세요.";
    }
    @Override public void close() {
        ScreenRouter.requireEdt(); if (closed) return;
        closed = true; generation++; session = null; refreshTimer.stop(); worker.shutdownNow(); panel.setSignedIn(false);
    }
}
