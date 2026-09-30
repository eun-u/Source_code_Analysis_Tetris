package kr.ac.jbnu.se.tetris.network.server;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kr.ac.jbnu.se.tetris.auth.AuthIdentity;
import kr.ac.jbnu.se.tetris.auth.AuthException;
import kr.ac.jbnu.se.tetris.auth.TokenVerifier;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.WebSocketNetworkClient;
import kr.ac.jbnu.se.tetris.network.protocol.WireCodec;
import kr.ac.jbnu.se.tetris.network.protocol.WireRequest;
import kr.ac.jbnu.se.tetris.ranking.MatchRecord;
import kr.ac.jbnu.se.tetris.ranking.RankedMatchStore;

/** A live loopback WebSocket transport with multiple independent ranked 1v1 rooms. */
public final class RenderGameServerTest {
    private static final String ADMIN = "local-test-admin-key-1234567890";
    private static final String[] USERS = {
            "66bb5a13-f250-4aa5-aa75-913cc7430001", "66bb5a13-f250-4aa5-aa75-913cc7430002",
            "66bb5a13-f250-4aa5-aa75-913cc7430003", "66bb5a13-f250-4aa5-aa75-913cc7430004",
            "66bb5a13-f250-4aa5-aa75-913cc7430005", "66bb5a13-f250-4aa5-aa75-913cc7430006"
    };

    public static void main(String[] args) throws Exception {
        TestStore store = new TestStore();
        TokenVerifier verifier = token -> {
            for (String user : USERS) if (user.equals(token)) {
                return new AuthIdentity(user, "Player", System.currentTimeMillis() / 1000L + 3600);
            }
            throw new IOException("Bad token");
        };
        RenderGameServer server = new RenderGameServer(verifier, store, ADMIN, 0);
        server.start();
        Probe[] probes = new Probe[6];
        try {
            String base = "http://127.0.0.1:" + server.getPort();
            assertEquals(200, request(base + "/healthz", "GET", null));
            assertEquals(401, request(base + "/admin/status", "GET", null));
            assertEquals(503, request(base + "/ws", "GET", "Bearer " + USERS[0]));
            assertEquals(200, request(base + "/admin/open", "POST", "Bearer " + ADMIN));
            fragmentedAndMalformedBoundary(server.getPort());
            oversizeFrameBoundary(server.getPort());
            for (int i = 0; i < probes.length; i++) {
                probes[i] = new Probe(USERS[i]);
                probes[i].client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:"
                        + server.getPort() + "/ws"), USERS[i]));
                probes[i].await(NetworkUpdate.Type.CONNECTED);
            }
            Probe duplicate = new Probe(USERS[0]);
            try {
                duplicate.client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:"
                        + server.getPort() + "/ws"), USERS[0]));
                if (duplicate.awaitConnectionOutcome() != NetworkUpdate.Type.CONNECTION_FAILED) {
                    throw new AssertionError("Duplicate account connected");
                }
            } finally { duplicate.client.close(); }
            long refreshId = probes[0].client.refreshAuthentication(USERS[0]);
            if (!probes[0].awaitOutcome(refreshId).getRequestOutcome().isAccepted()) {
                throw new AssertionError("Same-account token refresh rejected");
            }
            long forgedId = probes[0].client.refreshAuthentication(USERS[1]);
            if (probes[0].awaitOutcome(forgedId).getRequestOutcome().isAccepted()) {
                throw new AssertionError("Cross-account token refresh accepted");
            }
            for (int pair = 0; pair < 3; pair++) {
                Probe first = probes[pair * 2], second = probes[pair * 2 + 1];
                first.client.send(RoomCommand.createRoom(2));
                NetworkUpdate created = first.await(NetworkUpdate.Type.ROOM_STATE);
                second.client.send(RoomCommand.joinRoom(created.getRoomId()));
                second.await(NetworkUpdate.Type.ROOM_STATE);
                first.client.send(RoomCommand.setReady(true));
                second.client.send(RoomCommand.setReady(true));
                first.await(NetworkUpdate.Type.MATCH_STARTED);
                second.await(NetworkUpdate.Type.MATCH_STARTED);
            }
            if (store.records.size() != 3) throw new AssertionError("Three ranked matches not registered");
            String liveStatus = requestText(base + "/admin/status", "Bearer " + ADMIN);
            if (!liveStatus.contains("users=6") || !liveStatus.contains("rooms=3")
                    || !liveStatus.contains("active=3")) {
                throw new AssertionError("Three concurrent 1v1 matches not active: " + liveStatus);
            }
            for (int pair = 0; pair < 3; pair++) {
                Probe first = probes[pair * 2], second = probes[pair * 2 + 1];
                first.client.send(RoomCommand.leaveRoom());
                NetworkUpdate saved = second.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "SAVED");
                MatchRecord match = store.records.get(saved.getMatchId());
                if (match == null || match.getStatus() != MatchRecord.Status.FINALIZED
                        || !second.user.equals(match.getWinnerUserId())) {
                    throw new AssertionError("Forfeit did not persist winner once");
                }
                if (pair == 0) assertStatusContains(base, "active=2", "unresolved=2");
            }
            assertEquals(200, request(base + "/admin/drain", "POST", "Bearer " + ADMIN));
            Probe first = probes[0];
            long rejectedId = first.client.send(RoomCommand.createRoom(2));
            NetworkUpdate rejected = first.awaitOutcome(rejectedId);
            if (rejected.getRequestOutcome().isAccepted()) throw new AssertionError("Draining accepted room");
        } finally {
            for (Probe probe : probes) if (probe != null) probe.client.close();
            server.close();
        }
        staleRegistrationMustNotPoisonReplacementRoom(verifier);
        parallelAdmissionAndAuthErrors();
        configuredSocketLimitRejectsExcess(verifier);
        unauthenticatedTimeoutReleasesCapacity();
        invalidatedMatchIsTerminal(verifier);
        lostFinishResponseKeepsOrder(verifier);
        orphanPendingVisibleInDrainStatus(verifier);
        slowAuthRefreshMustNotBlockRanking();
        System.out.println("RenderGameServerTest: PASS");
    }

    private static void slowAuthRefreshMustNotBlockRanking() throws Exception {
        CountDownLatch refreshEntered = new CountDownLatch(2);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        TokenVerifier verifier = token -> {
            boolean refresh = token.startsWith("refresh-");
            String user = refresh ? token.substring("refresh-".length()) : token;
            if (!USERS[0].equals(user) && !USERS[1].equals(user)) throw new AuthException("INVALID_TOKEN");
            if (refresh) {
                refreshEntered.countDown();
                try { releaseRefresh.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
            return new AuthIdentity(user, "Player", System.currentTimeMillis() / 1000L + 3600);
        };
        RenderGameServer server = new RenderGameServer(verifier, new TestStore(), ADMIN, 0);
        server.start();
        Probe a = null, b = null;
        try {
            assertEquals(200, request("http://127.0.0.1:" + server.getPort() + "/admin/open",
                    "POST", "Bearer " + ADMIN));
            a = connectProbe(USERS[0], server.getPort());
            b = connectProbe(USERS[1], server.getPort());
            a.client.send(RoomCommand.createRoom(2));
            String roomId = a.await(NetworkUpdate.Type.ROOM_STATE).getRoomId();
            b.client.send(RoomCommand.joinRoom(roomId));
            b.await(NetworkUpdate.Type.ROOM_STATE);
            a.client.send(RoomCommand.setReady(true));
            b.client.send(RoomCommand.setReady(true));
            a.await(NetworkUpdate.Type.MATCH_STARTED);
            b.await(NetworkUpdate.Type.MATCH_STARTED);
            a.client.refreshAuthentication("refresh-" + USERS[0]);
            b.client.refreshAuthentication("refresh-" + USERS[1]);
            if (!refreshEntered.await(3, TimeUnit.SECONDS)) throw new AssertionError("Refreshes not in flight");
            long duplicateRefresh = b.client.refreshAuthentication("refresh-" + USERS[1]);
            NetworkUpdate denied = b.awaitOutcome(duplicateRefresh);
            if (denied.getRequestOutcome().isAccepted()
                    || !"AUTH_REFRESH_BUSY".equals(denied.getRequestOutcome().getReasonCode())) {
                throw new AssertionError("Per-peer refresh was not bounded");
            }
            a.client.send(RoomCommand.leaveRoom());
            b.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "SAVED");
        } finally {
            releaseRefresh.countDown();
            if (a != null) a.client.close();
            if (b != null) b.client.close();
            server.close();
        }
    }

    private static void lostFinishResponseKeepsOrder(TokenVerifier verifier) throws Exception {
        DelayedFinishStore store = new DelayedFinishStore();
        RenderGameServer server = new RenderGameServer(verifier, store, ADMIN, 0);
        server.start();
        Probe a = null, b = null;
        try {
            assertEquals(200, request("http://127.0.0.1:" + server.getPort() + "/admin/open",
                    "POST", "Bearer " + ADMIN));
            a = connectProbe(USERS[0], server.getPort());
            b = connectProbe(USERS[1], server.getPort());
            a.client.send(RoomCommand.createRoom(2));
            String roomId = a.await(NetworkUpdate.Type.ROOM_STATE).getRoomId();
            b.client.send(RoomCommand.joinRoom(roomId));
            b.await(NetworkUpdate.Type.ROOM_STATE);
            a.client.send(RoomCommand.setReady(true));
            b.client.send(RoomCommand.setReady(true));
            a.await(NetworkUpdate.Type.MATCH_STARTED);
            b.await(NetworkUpdate.Type.MATCH_STARTED);
            a.client.send(RoomCommand.leaveRoom());
            b.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "SAVE_PENDING");
            if (!store.firstFinished.await(5, TimeUnit.SECONDS)) throw new AssertionError("Finish was not sent");
            long rematchId = b.client.send(RoomCommand.requestRematch());
            NetworkUpdate denied = b.awaitOutcome(rematchId);
            if (denied.getRequestOutcome().isAccepted()
                    || !"RESULT_UNRESOLVED".equals(denied.getRequestOutcome().getReasonCode())) {
                throw new AssertionError("Rematch started before saved result was confirmed");
            }
            store.releaseResponse.countDown();
            NetworkUpdate saved = b.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "SAVED");
            if (store.getMatch(saved.getMatchId()).getStatus() != MatchRecord.Status.FINALIZED
                    || store.finishCalls.get() != 1) {
                throw new AssertionError("Lost DB response duplicated result or blocked recovery");
            }
        } finally {
            store.releaseResponse.countDown();
            if (a != null) a.client.close();
            if (b != null) b.client.close();
            server.close();
        }
    }

    private static void orphanPendingVisibleInDrainStatus(TokenVerifier verifier) throws Exception {
        DelayedFinishStore store = new DelayedFinishStore();
        RenderGameServer server = new RenderGameServer(verifier, store, ADMIN, 0);
        server.start();
        Probe a = null, b = null;
        try {
            String base = "http://127.0.0.1:" + server.getPort();
            assertEquals(200, request(base + "/admin/open", "POST", "Bearer " + ADMIN));
            a = connectProbe(USERS[0], server.getPort());
            b = connectProbe(USERS[1], server.getPort());
            a.client.send(RoomCommand.createRoom(2));
            String roomId = a.await(NetworkUpdate.Type.ROOM_STATE).getRoomId();
            b.client.send(RoomCommand.joinRoom(roomId));
            b.await(NetworkUpdate.Type.ROOM_STATE);
            a.client.send(RoomCommand.setReady(true));
            b.client.send(RoomCommand.setReady(true));
            a.await(NetworkUpdate.Type.MATCH_STARTED);
            b.await(NetworkUpdate.Type.MATCH_STARTED);
            a.client.send(RoomCommand.leaveRoom());
            b.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "SAVE_PENDING");
            if (!store.firstFinished.await(5, TimeUnit.SECONDS)) throw new AssertionError("Finish not pending");
            long lastLeave = b.client.send(RoomCommand.leaveRoom());
            b.awaitOutcome(lastLeave);
            assertStatusContains(base, "rooms=0", "unresolved=1");
            store.releaseResponse.countDown();
            assertStatusContains(base, "rooms=0", "unresolved=0");
        } finally {
            store.releaseResponse.countDown();
            if (a != null) a.client.close();
            if (b != null) b.client.close();
            server.close();
        }
    }

    private static void assertStatusContains(String base, String first, String second) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        String status = "";
        while (System.nanoTime() < until) {
            status = requestText(base + "/admin/status", "Bearer " + ADMIN);
            if (status.contains(first) && status.contains(second)) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Status missed " + first + "/" + second + ": " + status);
    }

    private static void invalidatedMatchIsTerminal(TokenVerifier verifier) throws Exception {
        TestStore store = new TestStore();
        RenderGameServer server = new RenderGameServer(verifier, store, ADMIN, 0);
        server.start();
        Probe a = null, b = null;
        try {
            assertEquals(200, request("http://127.0.0.1:" + server.getPort() + "/admin/open",
                    "POST", "Bearer " + ADMIN));
            a = connectProbe(USERS[0], server.getPort());
            b = connectProbe(USERS[1], server.getPort());
            a.client.send(RoomCommand.createRoom(2));
            String roomId = a.await(NetworkUpdate.Type.ROOM_STATE).getRoomId();
            b.client.send(RoomCommand.joinRoom(roomId));
            b.await(NetworkUpdate.Type.ROOM_STATE);
            a.client.send(RoomCommand.setReady(true));
            b.client.send(RoomCommand.setReady(true));
            a.await(NetworkUpdate.Type.MATCH_STARTED);
            b.await(NetworkUpdate.Type.MATCH_STARTED);

            // Force the same private failure transition used by a tick exception.
            java.lang.reflect.Field roomsField = RenderGameServer.class.getDeclaredField("rooms");
            java.lang.reflect.Field queueField = RenderGameServer.class.getDeclaredField("roomsExecutor");
            roomsField.setAccessible(true);
            queueField.setAccessible(true);
            java.util.concurrent.ScheduledThreadPoolExecutor queue =
                    (java.util.concurrent.ScheduledThreadPoolExecutor) queueField.get(server);
            Class<?> roomClass = Class.forName(RenderGameServer.class.getName() + "$Room");
            java.lang.reflect.Method invalidate = RenderGameServer.class.getDeclaredMethod("invalidate",
                    roomClass, String.class);
            invalidate.setAccessible(true);
            queue.submit(() -> {
                try {
                    Map<?, ?> current = (Map<?, ?>) roomsField.get(server);
                    invalidate.invoke(server, current.get(roomId), "SERVER_TICK_FAILED");
                } catch (Exception failure) { throw new RuntimeException(failure); }
            }).get(3, TimeUnit.SECONDS);
            for (Probe probe : new Probe[] { a, b }) {
                probe.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "VOIDED");
                NetworkUpdate terminal = probe.await(NetworkUpdate.Type.ERROR);
                if (!"MATCH_VOIDED".equals(terminal.getReasonCode())) {
                    throw new AssertionError("Missing terminal void error");
                }
            }
        } finally {
            if (a != null) a.client.close();
            if (b != null) b.client.close();
            server.close();
        }
    }

    private static void parallelAdmissionAndAuthErrors() throws Exception {
        CountDownLatch verifying = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        TokenVerifier verifier = token -> {
            if ("bad-token".equals(token)) throw new AuthException("INVALID_TOKEN");
            if ("down-token".equals(token)) throw new IOException("Auth backend unavailable");
            for (String user : USERS) if (user.equals(token)) {
                verifying.countDown();
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                return new AuthIdentity(user, "Player", System.currentTimeMillis() / 1000L + 3600);
            }
            throw new AuthException("INVALID_TOKEN");
        };
        RenderGameServer server = new RenderGameServer(verifier, new TestStore(), ADMIN, 0, 8, 2);
        server.start();
        Probe[] probes = new Probe[6];
        Probe recovered = null;
        try {
            String base = "http://127.0.0.1:" + server.getPort();
            assertEquals(200, request(base + "/admin/open", "POST", "Bearer " + ADMIN));
            assertEquals(401, request(base + "/ws", "GET", "Bearer bad-token"));
            assertEquals(503, request(base + "/ws", "GET", "Bearer down-token"));
            for (String[] caseData : new String[][] { { "bad-token", "AUTH_INVALID" },
                    { "down-token", "SERVICE_UNAVAILABLE" } }) {
                Probe failed = new Probe(caseData[0]);
                try {
                    failed.client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:"
                            + server.getPort() + "/ws"), caseData[0]));
                    NetworkUpdate update = failed.awaitConnectionUpdate();
                    if (update.getType() != NetworkUpdate.Type.CONNECTION_FAILED
                            || !caseData[1].equals(update.getReasonCode())) {
                        throw new AssertionError("Wrong handshake error for " + caseData[0]);
                    }
                } finally { failed.client.close(); }
            }
            for (int i = 0; i < 2; i++) {
                probes[i] = new Probe(USERS[i]);
                probes[i].client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:"
                        + server.getPort() + "/ws"), USERS[i]));
            }
            if (!verifying.await(5, TimeUnit.SECONDS)) throw new AssertionError("No parallel auth requests");
            for (int i = 2; i < probes.length; i++) {
                probes[i] = new Probe(USERS[i]);
                probes[i].client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:"
                        + server.getPort() + "/ws"), USERS[i]));
                if (probes[i].awaitConnectionOutcome() != NetworkUpdate.Type.CONNECTION_FAILED) {
                    throw new AssertionError("Pending-auth overload was accepted");
                }
            }
            release.countDown();
            int connected = 0;
            for (int i = 0; i < 2; i++) {
                NetworkUpdate.Type result = probes[i].awaitConnectionOutcome();
                if (result == NetworkUpdate.Type.CONNECTED) connected++;
            }
            if (connected != 2) throw new AssertionError("Expected two initial authentications");
            recovered = connectProbe(USERS[2], server.getPort());
        } finally {
            release.countDown();
            if (recovered != null) recovered.client.close();
            for (Probe probe : probes) if (probe != null) probe.client.close();
            server.close();
        }
    }

    private static void configuredSocketLimitRejectsExcess(TokenVerifier verifier) throws Exception {
        RenderGameServer server = new RenderGameServer(verifier, new TestStore(), ADMIN, 0, 2, 1);
        server.start();
        Probe first = null, second = null, excess = null, recovered = null;
        try {
            String base = "http://127.0.0.1:" + server.getPort();
            assertEquals(200, request(base + "/admin/open",
                    "POST", "Bearer " + ADMIN));
            first = connectProbe(USERS[0], server.getPort());
            second = connectProbe(USERS[1], server.getPort());
            assertEquals(200, request(base + "/healthz", "GET", null));
            if (!requestText(base + "/admin/status", "Bearer " + ADMIN).contains("users=2")) {
                throw new AssertionError("Admin status unavailable at WebSocket limit");
            }
            excess = new Probe(USERS[2]);
            excess.client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:"
                    + server.getPort() + "/ws"), USERS[2]));
            if (excess.awaitConnectionOutcome() != NetworkUpdate.Type.CONNECTION_FAILED) {
                throw new AssertionError("Configured socket bound was not applied");
            }
            assertEquals(200, request(base + "/admin/drain", "POST", "Bearer " + ADMIN));
            first.client.close();
            assertStatusContains(base, "users=1", "admission=false");
            assertEquals(200, request(base + "/admin/open", "POST", "Bearer " + ADMIN));
            recovered = connectProbe(USERS[2], server.getPort());
        } finally {
            if (first != null) first.client.close();
            if (second != null) second.client.close();
            if (excess != null) excess.client.close();
            if (recovered != null) recovered.client.close();
            server.close();
        }
    }

    private static void unauthenticatedTimeoutReleasesCapacity() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TokenVerifier slow = token -> {
            entered.countDown();
            try { release.await(22, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            return new AuthIdentity(USERS[0], "Player", System.currentTimeMillis() / 1000L + 3600);
        };
        RenderGameServer server = new RenderGameServer(slow, new TestStore(), ADMIN, 0, 8, 1);
        server.start();
        Probe recovered = null;
        try {
            assertEquals(200, request("http://127.0.0.1:" + server.getPort() + "/admin/open",
                    "POST", "Bearer " + ADMIN));
            try (Socket socket = new Socket("127.0.0.1", server.getPort())) {
                socket.setSoTimeout(22000);
                sendUpgrade(socket, server.getPort(), USERS[0]);
                if (!entered.await(3, TimeUnit.SECONDS)) throw new AssertionError("Auth verification not entered");
                if (socket.getInputStream().read() != -1) {
                    throw new AssertionError("Unauthenticated connection not closed by timeout");
                }
            }
            release.countDown();
            recovered = connectProbe(USERS[0], server.getPort());
        } finally {
            release.countDown();
            if (recovered != null) recovered.client.close();
            server.close();
        }
    }

    private static void staleRegistrationMustNotPoisonReplacementRoom(TokenVerifier verifier) throws Exception {
        FlakyStore store = new FlakyStore();
        RenderGameServer server = new RenderGameServer(verifier, store, ADMIN, 0);
        server.start();
        Probe a = null, b = null, c = null;
        try {
            String base = "http://127.0.0.1:" + server.getPort();
            assertEquals(200, request(base + "/admin/open", "POST", "Bearer " + ADMIN));
            a = connectProbe(USERS[0], server.getPort());
            b = connectProbe(USERS[1], server.getPort());
            c = connectProbe(USERS[2], server.getPort());
            a.client.send(RoomCommand.createRoom(2));
            String roomId = a.await(NetworkUpdate.Type.ROOM_STATE).getRoomId();
            b.client.send(RoomCommand.joinRoom(roomId));
            b.await(NetworkUpdate.Type.ROOM_STATE);
            a.client.send(RoomCommand.setReady(true));
            b.client.send(RoomCommand.setReady(true));
            if (!store.firstEntered.await(5, TimeUnit.SECONDS)) throw new AssertionError("First begin not entered");
            long leaveId = b.client.send(RoomCommand.leaveRoom());
            if (!b.awaitOutcome(leaveId).getRequestOutcome().isAccepted()) {
                throw new AssertionError("Departure during registration rejected");
            }
            c.client.send(RoomCommand.joinRoom(roomId));
            c.await(NetworkUpdate.Type.ROOM_STATE);
            a.client.send(RoomCommand.setReady(true));
            c.client.send(RoomCommand.setReady(true));
            a.await(NetworkUpdate.Type.MATCH_STARTED);
            c.await(NetworkUpdate.Type.MATCH_STARTED);
            store.releaseFirst.countDown();
            if (!store.firstReturned.await(5, TimeUnit.SECONDS)) throw new AssertionError("Old begin still pending");
            Thread.sleep(150);
            assertStatusContains(base, "rooms=1", "unresolved=2");
            store.commitLate();
            a.client.send(RoomCommand.leaveRoom());
            c.await(NetworkUpdate.Type.RANKED_SAVE_STATUS, "SAVED");
            assertStatusContains(base, "rooms=1", "unresolved=0");
        } finally {
            store.releaseFirst.countDown();
            if (a != null) a.client.close();
            if (b != null) b.client.close();
            if (c != null) c.client.close();
            server.close();
        }
    }

    private static Probe connectProbe(String user, int port) throws Exception {
        Probe probe = new Probe(user);
        probe.client.connect(new ConnectionOptions(new URI("ws://127.0.0.1:" + port + "/ws"), user));
        probe.await(NetworkUpdate.Type.CONNECTED);
        return probe;
    }

    private static int request(String url, String method, String auth) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        if (auth != null) connection.setRequestProperty("Authorization", auth);
        int result = connection.getResponseCode();
        connection.disconnect();
        return result;
    }

    private static String requestText(String url, String auth) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("Authorization", auth);
        if (connection.getResponseCode() != 200) throw new IOException("Status request rejected");
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[512];
            int count;
            while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally { connection.disconnect(); }
    }

    private static void fragmentedAndMalformedBoundary(int port) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(3000);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();
            sendUpgrade(socket, port, USERS[0]);
            String response = readHeaders(input);
            if (!response.startsWith("HTTP/1.1 101")) throw new AssertionError("WS upgrade failed: " + response);
            if (readRawUpdate(input).getType() != NetworkUpdate.Type.CONNECTED) {
                throw new AssertionError("Expected authenticated connected update");
            }
            byte[] room = WireCodec.encodeRequestPayload(WireRequest.room(1, RoomCommand.createRoom(2)));
            int midpoint = room.length / 2;
            sendMaskedFrame(output, 0x01, false, java.util.Arrays.copyOfRange(room, 0, midpoint));
            sendMaskedFrame(output, 0x00, true, java.util.Arrays.copyOfRange(room, midpoint, room.length));
            NetworkUpdate update;
            do { update = readRawUpdate(input); }
            while (update.getType() != NetworkUpdate.Type.ROOM_STATE);
            sendMaskedFrame(output, 0x01, true, "{bad".getBytes(StandardCharsets.UTF_8));
            if (input.read() != -1) throw new AssertionError("Malformed WS request was not closed");
        }
    }

    private static void oversizeFrameBoundary(int port) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(3000);
            sendUpgrade(socket, port, USERS[0]);
            if (!readHeaders(socket.getInputStream()).startsWith("HTTP/1.1 101")) {
                throw new AssertionError("WS upgrade failed");
            }
            readRawUpdate(socket.getInputStream());
            OutputStream output = socket.getOutputStream();
            output.write(0x81);
            output.write(0xff); // mask bit + 64-bit length
            long length = WireCodec.MAX_FRAME_BYTES + 1L;
            for (int shift = 56; shift >= 0; shift -= 8) output.write((int) (length >>> shift));
            output.write(new byte[] { 1, 2, 3, 4 });
            output.flush();
            int response = socket.getInputStream().read();
            if (response != -1 && (response & 0x0f) != 8) {
                throw new AssertionError("Oversize frame was not rejected");
            }
        }
    }

    private static void sendUpgrade(Socket socket, int port, String token) throws IOException {
        String key = Base64.getEncoder().encodeToString(new byte[16]);
        String upgrade = "GET /ws HTTP/1.1\r\nHost: 127.0.0.1:" + port
                + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + key
                + "\r\nSec-WebSocket-Version: 13\r\nAuthorization: Bearer " + token + "\r\n\r\n";
        socket.getOutputStream().write(upgrade.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
    }

    private static String readHeaders(InputStream input) throws IOException {
        StringBuilder text = new StringBuilder();
        while (text.length() < 8192) {
            int next = input.read();
            if (next == -1) throw new IOException("Closed before headers");
            text.append((char) next);
            if (text.toString().endsWith("\r\n\r\n")) return text.toString();
        }
        throw new IOException("Header too large");
    }

    private static NetworkUpdate readRawUpdate(InputStream input) throws IOException {
        int first = input.read(), second = input.read();
        if (first == -1 || second == -1) throw new IOException("Closed before update");
        int length = second & 0x7f;
        if (length == 126) length = (input.read() << 8) | input.read();
        else if (length == 127) throw new IOException("Unexpected long update");
        byte[] payload = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(payload, offset, length - offset);
            if (count < 0) throw new IOException("Truncated update");
            offset += count;
        }
        return WireCodec.decodeUpdatePayload(payload);
    }

    private static void sendMaskedFrame(OutputStream output, int opcode, boolean fin, byte[] payload)
            throws IOException {
        output.write((fin ? 0x80 : 0) | opcode);
        if (payload.length < 126) output.write(0x80 | payload.length);
        else if (payload.length <= 65535) {
            output.write(0x80 | 126);
            output.write(payload.length >>> 8);
            output.write(payload.length);
        } else throw new IllegalArgumentException("Test frame too large");
        byte[] mask = { 1, 2, 3, 4 };
        output.write(mask);
        for (int i = 0; i < payload.length; i++) output.write(payload[i] ^ mask[i & 3]);
        output.flush();
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static final class Probe {
        private final String user;
        private final WebSocketNetworkClient client = new WebSocketNetworkClient();
        private final ArrayBlockingQueue<NetworkUpdate> updates = new ArrayBlockingQueue<NetworkUpdate>(512);
        private Probe(String user) {
            this.user = user;
            client.subscribe(update -> updates.offer(update));
        }
        private NetworkUpdate await(NetworkUpdate.Type type) throws Exception { return await(type, null); }
        private NetworkUpdate.Type awaitConnectionOutcome() throws Exception {
            return awaitConnectionUpdate().getType();
        }
        private NetworkUpdate awaitConnectionUpdate() throws Exception {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < until) {
                NetworkUpdate update = updates.poll(100, TimeUnit.MILLISECONDS);
                if (update == null) continue;
                if (update.getType() == NetworkUpdate.Type.CONNECTED
                        || update.getType() == NetworkUpdate.Type.CONNECTION_FAILED) return update;
            }
            throw new AssertionError("Timed out waiting for connection outcome");
        }
        private NetworkUpdate awaitOutcome(long requestId) throws Exception {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < until) {
                NetworkUpdate update = updates.poll(100, TimeUnit.MILLISECONDS);
                if (update == null) continue;
                if (update.getType() == NetworkUpdate.Type.REQUEST_OUTCOME
                        && update.getRequestOutcome().getRequestId() == requestId) return update;
            }
            throw new AssertionError("Timed out waiting for request " + requestId);
        }
        private NetworkUpdate await(NetworkUpdate.Type type, String reason) throws Exception {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < until) {
                NetworkUpdate update = updates.poll(100, TimeUnit.MILLISECONDS);
                if (update == null) continue;
                if (update.getType() == NetworkUpdate.Type.CONNECTION_FAILED
                        || update.getType() == NetworkUpdate.Type.CLOSED) {
                    throw new AssertionError("Unexpected disconnect: " + update.getReasonCode());
                }
                if (update.getType() == type && (reason == null || reason.equals(update.getReasonCode()))) return update;
            }
            throw new AssertionError("Timed out waiting for " + type + " " + reason);
        }
    }

    private static class TestStore implements RankedMatchStore {
        protected final Map<String, MatchRecord> records = new ConcurrentHashMap<String, MatchRecord>();
        @Override public synchronized MatchRecord beginMatch(String matchId, String runId, String rulesVersion,
                String first, String second) throws IOException {
            if (first.equals(second)) throw new IOException("self match");
            MatchRecord existing = records.get(matchId);
            if (existing != null) return existing;
            for (MatchRecord record : records.values()) {
                if (record.getStatus() == MatchRecord.Status.RUNNING
                        && (first.equals(record.getFirstUserId()) || second.equals(record.getFirstUserId())
                        || first.equals(record.getSecondUserId()) || second.equals(record.getSecondUserId()))) {
                    throw new IOException("active match");
                }
            }
            MatchRecord record = new MatchRecord(matchId, runId, first, second,
                    MatchRecord.Status.RUNNING, null, null, null, null, null, null);
            records.put(matchId, record);
            return record;
        }
        @Override public synchronized MatchRecord finishMatch(String matchId, String runId,
                String winner, String reason) throws IOException {
            MatchRecord old = records.get(matchId);
            if (old == null || !old.getServerRunId().equals(runId)) throw new IOException("Unknown match");
            if (old.getStatus() == MatchRecord.Status.FINALIZED) return old;
            if (old.getStatus() == MatchRecord.Status.VOID) throw new IOException("Voided match");
            MatchRecord next = new MatchRecord(matchId, runId, old.getFirstUserId(), old.getSecondUserId(),
                    MatchRecord.Status.FINALIZED, winner, reason, 1000, 1016, 1000, 984);
            records.put(matchId, next);
            return next;
        }
        @Override public synchronized MatchRecord voidMatch(String matchId, String runId,
                String reason) throws IOException {
            MatchRecord old = records.get(matchId);
            if (old == null || !old.getServerRunId().equals(runId)) throw new IOException("Unknown match");
            if (old.getStatus() == MatchRecord.Status.FINALIZED) throw new IOException("Finalized match");
            MatchRecord next = new MatchRecord(matchId, runId, old.getFirstUserId(), old.getSecondUserId(),
                    MatchRecord.Status.VOID, null, reason, null, null, null, null);
            records.put(matchId, next);
            return next;
        }
        @Override public synchronized MatchRecord getMatch(String matchId) throws IOException {
            return records.get(matchId);
        }
        @Override public int voidStoppedRun(String stoppedRunId) { return 0; }
    }

    private static final class FlakyStore extends TestStore {
        private final AtomicInteger beginCalls = new AtomicInteger();
        private final CountDownLatch firstEntered = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);
        private final CountDownLatch firstReturned = new CountDownLatch(1);
        private volatile String firstMatchId;
        private volatile String firstRunId;
        private volatile String firstUserId;
        private volatile String secondUserId;
        private final AtomicInteger firstReads = new AtomicInteger();

        @Override public MatchRecord beginMatch(String matchId, String runId, String rulesVersion,
                String first, String second) throws IOException {
            if (beginCalls.incrementAndGet() == 1) {
                firstMatchId = matchId;
                firstRunId = runId;
                firstUserId = first;
                secondUserId = second;
                firstEntered.countDown();
                try { releaseFirst.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                firstReturned.countDown();
                throw new IOException("Lost first registration response");
            }
            return super.beginMatch(matchId, runId, rulesVersion, first, second);
        }

        @Override public synchronized MatchRecord getMatch(String matchId) throws IOException {
            if (matchId.equals(firstMatchId) && firstReads.getAndIncrement() == 0) {
                throw new IOException("Temporary lookup failure");
            }
            return super.getMatch(matchId);
        }

        private void commitLate() {
            records.put(firstMatchId, new MatchRecord(firstMatchId, firstRunId, firstUserId,
                    secondUserId, MatchRecord.Status.RUNNING, null, null, null, null, null, null));
        }
    }

    private static final class DelayedFinishStore extends TestStore {
        private final AtomicInteger finishCalls = new AtomicInteger();
        private final CountDownLatch firstFinished = new CountDownLatch(1);
        private final CountDownLatch releaseResponse = new CountDownLatch(1);

        @Override public MatchRecord finishMatch(String matchId, String runId, String winner,
                String reason) throws IOException {
            MatchRecord saved = super.finishMatch(matchId, runId, winner, reason);
            if (finishCalls.incrementAndGet() == 1) {
                firstFinished.countDown();
                try { releaseResponse.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                throw new IOException("Response lost after commit");
            }
            return saved;
        }
    }
}
