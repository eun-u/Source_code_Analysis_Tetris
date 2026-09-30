package kr.ac.jbnu.se.tetris.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.supabase.SupabaseConfig;

/** HTTP 지연 중 EDT 응답성, 토큰 갱신, 계정 전환 실패와 종료 후 응답 무시 검증 */
public final class OnlineAccountControllerTest {
    private static final String USER = "00000000-0000-4000-8000-000000000001";
    public static void main(String[] args) throws Exception {
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger refreshes = new AtomicInteger();
        api.createContext("/auth/v1/token", exchange -> {
            if (exchange.getRequestURI().getQuery().contains("refresh_token")) {
                refreshes.incrementAndGet(); reply(exchange, 200, session("fresh", 3600)); return;
            }
            entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test release timeout"); }
            catch (InterruptedException stopped) { Thread.currentThread().interrupt(); throw new IOException(stopped); }
            reply(exchange, 200, session("initial", 60));
        });
        api.createContext("/auth/v1/signup", exchange -> reply(exchange, 503, "{}"));
        api.createContext("/rest/v1/rpc/ranked_leaderboard", exchange -> {
            check("Bearer fresh".equals(exchange.getRequestHeaders().getFirst("Authorization")), "fresh token used for ranking");
            reply(exchange, 200, "[{\"rank\":1,\"user_id\":\"" + USER + "\",\"display_name\":\"Player\",\"rating\":1016,\"wins\":1,\"losses\":0,\"games\":1}]");
        });
        api.start(); OnlineAccountController[] controller = new OnlineAccountController[1];
        try {
            OnlineClientConfig config = new OnlineClientConfig(URI.create("ws://127.0.0.1:12345/ws"),
                    new SupabaseConfig("http://127.0.0.1:" + api.getAddress().getPort(), "public-test-key"));
            SwingUtilities.invokeAndWait(() -> {
                controller[0] = new OnlineAccountController(config, () -> { }, () -> { });
                ((JTextField) find(controller[0].getPanel(), "accountEmail")).setText("a@example.test");
                ((JPasswordField) find(controller[0].getPanel(), "accountPassword")).setText("password123");
                button(controller[0], "accountSignIn").doClick();
            });
            check(entered.await(3, TimeUnit.SECONDS), "HTTP call started off EDT");
            SwingUtilities.invokeAndWait(() -> {
                check(!button(controller[0], "accountSignIn").isEnabled(), "busy prevents duplicate login");
                check(((JPasswordField) find(controller[0].getPanel(), "accountPassword")).getPassword().length == 0, "password input cleared");
            });
            release.countDown();
            await(() -> controller[0].isSignedIn(), "login completion");
            SwingUtilities.invokeAndWait(() -> controller[0].refreshLeaderboard());
            await(() -> ((JTable) find(controller[0].getPanel(), "leaderboardTable")).getRowCount() == 1, "leaderboard completion");
            check(refreshes.get() == 1, "near expiry refreshed once before ranking");
            SwingUtilities.invokeAndWait(() -> {
                controller[0].signUp("new@example.test", "password123");
                check(!controller[0].isSignedIn(), "identity switching clears old UI session immediately");
            });
            await(() -> button(controller[0], "accountSignIn").isEnabled(), "failed signup completed");
            SwingUtilities.invokeAndWait(() -> {
                check(!controller[0].isSignedIn(), "failed signup does not restore stale identity");
                check(((JTable) find(controller[0].getPanel(), "leaderboardTable")).getRowCount() == 0, "stale account rows cleared");
                controller[0].signIn("a@example.test", "password123");
                controller[0].close();
            });
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> check(!controller[0].isSignedIn(), "late completion after close ignored"));
        } finally {
            release.countDown();
            SwingUtilities.invokeAndWait(() -> { if (controller[0] != null) controller[0].close(); });
            api.stop(0);
        }
        System.out.println("PASS OnlineAccountControllerTest: EDT, refresh, stale identity, close");
    }
    private static String session(String token, int ttl) {
        return "{\"access_token\":\"" + token + "\",\"refresh_token\":\"refresh-test\",\"expires_in\":" + ttl
                + ",\"user\":{\"id\":\"" + USER + "\"}}";
    }
    private static void reply(HttpExchange request, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); request.sendResponseHeaders(status, bytes.length);
        request.getResponseBody().write(bytes); request.close();
    }
    private static JButton button(OnlineAccountController controller, String name) { return (JButton) find(controller.getPanel(), name); }
    private static Component find(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container) { Component result = find((Container) component, name); if (result != null) return result; }
        }
        return null;
    }
    private static void await(BooleanSupplier predicate, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        boolean[] done = new boolean[1];
        do { SwingUtilities.invokeAndWait(() -> done[0] = predicate.getAsBoolean()); if (done[0]) return; Thread.sleep(10); }
        while (System.nanoTime() < deadline);
        throw new AssertionError(description);
    }
    private static void check(boolean value, String description) { if (!value) throw new AssertionError(description); }
}
