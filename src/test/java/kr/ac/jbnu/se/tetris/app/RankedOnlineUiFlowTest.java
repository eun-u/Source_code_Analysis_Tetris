package kr.ac.jbnu.se.tetris.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.auth.AuthException;
import kr.ac.jbnu.se.tetris.auth.AuthIdentity;
import kr.ac.jbnu.se.tetris.auth.TokenVerifier;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.server.RenderGameServer;
import kr.ac.jbnu.se.tetris.ranking.MatchRecord;
import kr.ac.jbnu.se.tetris.ranking.RankedMatchStore;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;

/** Headless Swing -> Auth -> WebSocket battle -> save gate -> leaderboard integration. */
public final class RankedOnlineUiFlowTest {
    private static final String ADMIN = "local-test-admin-key-1234567890";
    private static final String FIRST = "66bb5a13-f250-4aa5-aa75-913cc7430001";
    private static final String SECOND = "66bb5a13-f250-4aa5-aa75-913cc7430002";

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        DelayedStore store = new DelayedStore();
        TokenVerifier verifier = token -> {
            if (FIRST.equals(token) || SECOND.equals(token))
                return new AuthIdentity(token, "Player", System.currentTimeMillis() / 1000L + 3600);
            throw new AuthException("INVALID_TOKEN");
        };
        HttpServer auth = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        auth.createContext("/", request -> respondAuth(request, store));
        auth.start();
        RenderGameServer server = new RenderGameServer(verifier, store, ADMIN, 0);
        TetrisApplication[] apps = new TetrisApplication[2];
        try {
            server.start();
            int port = server.getPort();
            check(httpPost("http://127.0.0.1:" + port + "/admin/open", ADMIN) == 200, "admission open");
            OnlineClientConfig config = new OnlineClientConfig(
                    new URI("ws://127.0.0.1:" + port + "/ws"),
                    new SupabaseConfig("http://127.0.0.1:" + auth.getAddress().getPort(), "public-test-key"));
            edt(() -> {
                apps[0] = new TetrisApplication(new Random(1), config);
                apps[1] = new TetrisApplication(new Random(2), config);
                for (int i = 0; i < 2; i++) {
                    apps[i].showAccount();
                    field(apps[i], "accountEmail").setText(i == 0 ? "first@example.test" : "second@example.test");
                    ((JPasswordField) find(apps[i].getRouter().getContainer(), "accountPassword"))
                            .setText("test-password");
                    button(apps[i], "accountSignIn").doClick();
                }
            });
            awaitEdt(() -> button(apps[0], "accountSignOut").isEnabled()
                    && button(apps[1], "accountSignOut").isEnabled(), "both authenticated");
            edt(() -> {
                button(apps[0], "accountBack").doClick();
                button(apps[1], "accountBack").doClick();
                button(apps[0], "connectRanked").doClick();
                button(apps[1], "connectRanked").doClick();
            });
            awaitEdt(() -> button(apps[0], "createRoom").isEnabled()
                    && button(apps[1], "createRoom").isEnabled(), "both WebSocket clients connected");
            edt(() -> button(apps[0], "createRoom").doClick());
            awaitEdt(() -> !field(apps[0], "roomId").getText().isEmpty(), "room created");
            String roomId = onEdt(() -> field(apps[0], "roomId").getText());
            edt(() -> {
                field(apps[1], "roomId").setText(roomId);
                button(apps[1], "joinRoom").doClick();
            });
            awaitEdt(() -> button(apps[0], "roomReady").isEnabled()
                    && button(apps[1], "roomReady").isEnabled(), "joined room");
            edt(() -> { button(apps[0], "roomReady").doClick(); button(apps[1], "roomReady").doClick(); });
            awaitEdt(() -> running(apps[0]) && running(apps[1]), "ranked match started");
            edt(() -> {
                check(!apps[0].isGravityRunning() && !apps[0].isAiTimerRunning(), "no local match tick");
                apps[0].sendRoomCommand(RoomCommand.leaveRoom());
            });
            check(store.finishEntered.await(5, TimeUnit.SECONDS), "persistence reached");
            awaitEdt(() -> finished(apps[1]) && !button(apps[1], "retry").isEnabled()
                    && !((JLabel) find(apps[1].getRouter().getContainer(), "rankedSaveStatus"))
                            .getText().isEmpty(), "result waits for saved rating");
            check(store.records.size() == 1, "exactly one match registered");
            check(store.records.values().iterator().next().getStatus() == MatchRecord.Status.RUNNING,
                    "record still pending while DB held");
            // A held DB worker must leave the EDT responsive, including ordinary button state queries.
            check(onEdt(() -> !button(apps[1], "retry").isEnabled()), "EDT remains responsive");
            edt(() -> apps[0].close());
            check(onEdt(() -> !button(apps[1], "retry").isEnabled()),
                    "cancelling client closes without blocking the EDT");
            store.releaseFinish.countDown();
            awaitEdt(() -> button(apps[1], "retry").isEnabled()
                    && store.records.values().iterator().next().getStatus() == MatchRecord.Status.FINALIZED,
                    "saved result enables rematch");
            edt(() -> button(apps[1], "resultLeaderboard").doClick());
            awaitEdt(() -> "account".equals(apps[1].getRouter().getCurrentId())
                    && ((JTable) find(apps[1].getRouter().getContainer(), "leaderboardTable"))
                            .getRowCount() == 1, "leaderboard shows finalized winner");
            check(onEdt(() -> ((JTable) find(apps[1].getRouter().getContainer(), "leaderboardTable"))
                    .getValueAt(0, 2)).equals(1016), "leaderboard rating");
        } finally {
            store.releaseFinish.countDown();
            edt(() -> { for (TetrisApplication app : apps) if (app != null) app.close(); });
            server.close();
            auth.stop(0);
        }
        System.out.println("PASS RankedOnlineUiFlowTest");
    }

    private static boolean running(TetrisApplication app) {
        return "battle".equals(app.getRouter().getCurrentId()) && app.getBattleState() != null
                && app.getBattleState().getStatus() == BattleState.Status.RUNNING;
    }
    private static boolean finished(TetrisApplication app) {
        return "result".equals(app.getRouter().getCurrentId()) && app.getBattleState() != null
                && app.getBattleState().getStatus() == BattleState.Status.FINISHED;
    }

    private static void respondAuth(HttpExchange request, DelayedStore store) throws IOException {
        String path = request.getRequestURI().getPath();
        String body = new String(read(request), StandardCharsets.UTF_8);
        check("public-test-key".equals(request.getRequestHeaders().getFirst("apikey")), "public Auth key");
        if (path.equals("/auth/v1/token")) {
            String user = body.contains("first@example.test") ? FIRST : SECOND;
            String response = "{\"access_token\":\"" + user + "\",\"refresh_token\":\"r-" + user
                    + "\",\"expires_in\":3600,\"user\":{\"id\":\"" + user + "\"}}";
            answer(request, 200, response);
        } else if (path.equals("/rest/v1/rpc/ranked_leaderboard")) {
            check(("Bearer " + FIRST).equals(request.getRequestHeaders().getFirst("Authorization"))
                    || ("Bearer " + SECOND).equals(request.getRequestHeaders().getFirst("Authorization")),
                    "signed-in leaderboard request");
            boolean saved = store.records.values().stream().anyMatch(record ->
                    record.getStatus() == MatchRecord.Status.FINALIZED);
            answer(request, 200, saved ? "[{\"rank\":1,\"user_id\":\"" + SECOND
                    + "\",\"display_name\":\"Player\",\"rating\":1016,\"wins\":1,"
                    + "\"losses\":0,\"games\":1}]" : "[]");
        } else answer(request, 404, "{}");
    }

    private static byte[] read(HttpExchange request) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512]; int n;
            while ((n = request.getRequestBody().read(buffer)) != -1) output.write(buffer, 0, n);
            return output.toByteArray();
        }
    }
    private static void answer(HttpExchange request, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        request.sendResponseHeaders(status, bytes.length);
        request.getResponseBody().write(bytes); request.close();
    }
    private static int httpPost(String url, String token) throws IOException {
        HttpURLConnection request = (HttpURLConnection) new URL(url).openConnection();
        request.setRequestMethod("POST"); request.setRequestProperty("Authorization", "Bearer " + token);
        request.setConnectTimeout(2000); request.setReadTimeout(2000);
        try { return request.getResponseCode(); } finally { request.disconnect(); }
    }
    private static Component find(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container) {
                Component child = find((Container) component, name); if (child != null) return child;
            }
        }
        return null;
    }
    private static JButton button(TetrisApplication app, String name) {
        return (JButton) find(app.getRouter().getContainer(), name);
    }
    private static JTextField field(TetrisApplication app, String name) {
        return (JTextField) find(app.getRouter().getContainer(), name);
    }
    private static void edt(Runnable runnable) throws Exception {
        onEdt(() -> { runnable.run(); return true; });
    }
    private static <T> T onEdt(java.util.concurrent.Callable<T> work) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            try { value.set(work.call()); }
            catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        });
        check(done.await(2, TimeUnit.SECONDS), "EDT blocked");
        if (failure.get() != null) throw new AssertionError("EDT failure", failure.get());
        return value.get();
    }
    private static void awaitEdt(BooleanSupplier condition, String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!onEdt(() -> condition.getAsBoolean()) && System.nanoTime() < deadline)
            Thread.sleep(15);
        check(onEdt(() -> condition.getAsBoolean()), name);
    }
    private static void check(boolean valid, String name) {
        if (!valid) throw new AssertionError(name);
    }

    private static final class DelayedStore implements RankedMatchStore {
        final Map<String, MatchRecord> records = new ConcurrentHashMap<>();
        final CountDownLatch finishEntered = new CountDownLatch(1);
        final CountDownLatch releaseFinish = new CountDownLatch(1);
        @Override public synchronized MatchRecord beginMatch(String matchId, String runId,
                String rulesVersion, String first, String second) throws IOException {
            MatchRecord existing = records.get(matchId);
            if (existing != null) return existing;
            MatchRecord running = new MatchRecord(matchId, runId, first, second,
                    MatchRecord.Status.RUNNING, null, null, null, null, null, null);
            records.put(matchId, running);
            return running;
        }
        @Override public MatchRecord finishMatch(String matchId, String runId,
                String winner, String reason) throws IOException {
            finishEntered.countDown();
            try { if (!releaseFinish.await(8, TimeUnit.SECONDS)) throw new IOException("Delayed save timeout"); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException(error); }
            synchronized (this) {
                MatchRecord old = records.get(matchId);
                if (old == null) throw new IOException("Unknown match");
                if (old.getStatus() == MatchRecord.Status.FINALIZED) return old;
                MatchRecord saved = new MatchRecord(matchId, runId, old.getFirstUserId(), old.getSecondUserId(),
                        MatchRecord.Status.FINALIZED, winner, reason, 1000,
                        winner.equals(old.getFirstUserId()) ? 1016 : 984, 1000,
                        winner.equals(old.getSecondUserId()) ? 1016 : 984);
                records.put(matchId, saved); return saved;
            }
        }
        @Override public synchronized MatchRecord voidMatch(String matchId, String runId, String reason)
                throws IOException {
            MatchRecord old = records.get(matchId);
            if (old == null) throw new IOException("Unknown match");
            MatchRecord voided = new MatchRecord(matchId, runId, old.getFirstUserId(), old.getSecondUserId(),
                    MatchRecord.Status.VOID, null, reason, null, null, null, null);
            records.put(matchId, voided); return voided;
        }
        @Override public synchronized MatchRecord getMatch(String matchId) { return records.get(matchId); }
        @Override public int voidStoppedRun(String runId) { return 0; }
    }
}
