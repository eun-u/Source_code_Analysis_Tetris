package kr.ac.jbnu.se.tetris.network;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.protocol.WireCodec;
import kr.ac.jbnu.se.tetris.network.protocol.WireRequest;

/** Java 8 표준 TLS/WebSocket 연결. 클라이언트 프레임 마스킹과 응답 크기를 검증한다. */
public final class WebSocketNetworkClient implements NetworkClient {
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_HANDSHAKE = 8192;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AtomicLong nextId = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ArrayBlockingQueue<WireRequest> outgoing = new ArrayBlockingQueue<WireRequest>(256);
    private final ThreadPoolExecutor callbacks = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(256), task -> daemon(task, "tetris-ws-callback"));
    private final Object writeLock = new Object();
    private final List<Observer> observers = new ArrayList<Observer>();
    private NetworkUpdate connectedUpdate, roomUpdate, startUpdate, snapshotUpdate;
    private volatile Socket socket;
    private volatile Thread writer;
    private volatile boolean connected;
    private volatile String matchId;

    @Override public long connect(ConnectionOptions options) {
        requireOpen();
        if (options == null || !options.isWebSocket()) throw new IllegalArgumentException("WebSocket URI required");
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("ALREADY_CONNECTING");
        long id = nextId.incrementAndGet();
        daemon(() -> readConnection(options), "tetris-ws-reader").start();
        return id;
    }

    @Override public synchronized long send(RoomCommand command) {
        requireOpen();
        if (command == null) throw new IllegalArgumentException("Room command required");
        long id = nextId.incrementAndGet(); enqueue(WireRequest.room(id, command)); return id;
    }

    @Override public synchronized long send(PlayerIntent intent) {
        requireOpen();
        if (intent == null) throw new IllegalArgumentException("Player intent required");
        String current = matchId;
        if (current == null) throw new IllegalStateException("MATCH_NOT_STARTED");
        long id = nextId.incrementAndGet(); enqueue(WireRequest.intent(id, current, intent)); return id;
    }

    @Override public synchronized long requestSnapshot() {
        requireOpen();
        String current = matchId;
        if (current == null) throw new IllegalStateException("MATCH_NOT_STARTED");
        long id = nextId.incrementAndGet(); enqueue(WireRequest.snapshot(id, current)); return id;
    }

    @Override public synchronized long refreshAuthentication(String accessToken) {
        requireOpen();
        long id = nextId.incrementAndGet(); enqueue(WireRequest.authRefresh(id, accessToken)); return id;
    }

    private void enqueue(WireRequest request) {
        if (!connected) throw new IllegalStateException("NOT_CONNECTED");
        if (!outgoing.offer(request)) throw new IllegalStateException("SEND_QUEUE_FULL");
    }

    @Override public NetworkSubscription subscribe(NetworkListener listener) {
        requireOpen();
        if (listener == null) throw new IllegalArgumentException("Listener required");
        Observer observer = new Observer(listener);
        dispatch(() -> {
            if (!observer.active.get()) return;
            observers.add(observer);
            if (connectedUpdate != null) observer.deliver(connectedUpdate);
            if (roomUpdate != null) observer.deliver(roomUpdate);
            if (startUpdate != null) observer.deliver(startUpdate);
            if (snapshotUpdate != null) observer.deliver(snapshotUpdate);
        });
        return () -> {
            observer.active.set(false);
            if (!closed.get()) dispatch(() -> observers.remove(observer));
        };
    }

    private void readConnection(ConnectionOptions options) {
        boolean upgraded = false;
        try {
            Socket plain = new Socket();
            socket = plain;
            plain.connect(new InetSocketAddress(options.getHost(), options.getPort()), 5000);
            plain.setSoTimeout(90000);
            Socket opened = plain;
            if ("wss".equalsIgnoreCase(options.getWebSocketUri().getScheme())) {
                SSLSocket secured = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                        .createSocket(plain, options.getHost(), options.getPort(), true);
                SSLParameters parameters = secured.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                secured.setSSLParameters(parameters);
                secured.startHandshake();
                socket = secured;
                opened = secured;
            }
            if (closed.get()) { opened.close(); return; }
            handshake(opened, options);
            upgraded = true;
            connected = true;
            final Socket established = opened;
            writer = daemon(() -> writeConnection(established), "tetris-ws-writer");
            writer.start();
            readFrames(established);
            if (!closed.get()) terminate("CONNECTION_LOST", false);
        } catch (IOException | RuntimeException error) {
            if (!closed.get()) terminate(upgraded ? "CONNECTION_LOST" : classifyHandshake(error), !upgraded);
        }
    }

    private static String classifyHandshake(Exception error) {
        String message = error.getMessage();
        if ("AUTH_INVALID".equals(message) || "SERVICE_UNAVAILABLE".equals(message)) return message;
        if ("HANDSHAKE_FAILED".equals(message)) return message;
        return "CONNECTION_FAILED";
    }

    private static void handshake(Socket socket, ConnectionOptions options) throws IOException {
        byte[] nonce = new byte[16]; RANDOM.nextBytes(nonce);
        String key = java.util.Base64.getEncoder().encodeToString(nonce);
        URI uri = options.getWebSocketUri();
        String host = uri.getHost();
        boolean standardPort = options.getPort() == ("wss".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
        String hostHeader = host + (standardPort ? "" : ":" + options.getPort());
        String request = "GET /ws HTTP/1.1\r\nHost: " + hostHeader + "\r\nUpgrade: websocket\r\n"
                + "Connection: Upgrade\r\nSec-WebSocket-Key: " + key + "\r\nSec-WebSocket-Version: 13\r\n"
                + "Authorization: Bearer " + options.getAccessToken() + "\r\n\r\n";
        socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
        ByteArrayOutputStream headers = new ByteArrayOutputStream();
        InputStream input = socket.getInputStream();
        while (headers.size() < MAX_HANDSHAKE) {
            int next = input.read();
            if (next < 0) throw new EOFException("WebSocket handshake ended");
            headers.write(next);
            byte[] current = headers.toByteArray();
            int size = current.length;
            if (size >= 4 && current[size - 4] == '\r' && current[size - 3] == '\n'
                    && current[size - 2] == '\r' && current[size - 1] == '\n') break;
        }
        byte[] raw = headers.toByteArray();
        if (raw.length >= MAX_HANDSHAKE) throw new IOException("HANDSHAKE_FAILED");
        String response = new String(raw, StandardCharsets.US_ASCII);
        String[] lines = response.split("\r\n");
        if (lines.length < 2) throw new IOException("HANDSHAKE_FAILED");
        if (lines[0].matches("HTTP/1\\.[01] 401 .*")) throw new IOException("AUTH_INVALID");
        if (lines[0].matches("HTTP/1\\.[01] 503 .*")) throw new IOException("SERVICE_UNAVAILABLE");
        if (!lines[0].matches("HTTP/1\\.[01] 101 .*")) throw new IOException("HANDSHAKE_FAILED");
        String accept = null;
        boolean upgrade = false, connection = false;
        for (int index = 1; index < lines.length; index++) {
            int separator = lines[index].indexOf(':');
            if (separator < 1) continue;
            String name = lines[index].substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = lines[index].substring(separator + 1).trim();
            if ("sec-websocket-accept".equals(name)) accept = value;
            else if ("upgrade".equals(name)) upgrade = "websocket".equalsIgnoreCase(value);
            else if ("connection".equals(name)) connection = value.toLowerCase(Locale.ROOT).contains("upgrade");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((key + GUID).getBytes(StandardCharsets.US_ASCII));
            String expected = java.util.Base64.getEncoder().encodeToString(digest);
            if (!upgrade || !connection || !expected.equals(accept)) throw new IOException("HANDSHAKE_FAILED");
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IOException("HANDSHAKE_FAILED"); }
    }

    private void writeConnection(Socket opened) {
        try {
            while (!closed.get()) {
                WireRequest request = outgoing.take();
                byte[] payload = WireCodec.encodeRequestPayload(request);
                writeFrame(opened.getOutputStream(), 1, payload);
            }
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        catch (IOException | RuntimeException error) { if (!closed.get()) terminate("CONNECTION_LOST", false); }
    }

    private void readFrames(Socket opened) throws IOException {
        InputStream input = opened.getInputStream();
        ByteArrayOutputStream fragments = null;
        while (!closed.get()) {
            int first = input.read();
            int second = input.read();
            if (first < 0 || second < 0) throw new EOFException("WebSocket closed");
            if ((first & 0x70) != 0 || (second & 0x80) != 0) throw new IOException("Invalid server frame");
            boolean fin = (first & 0x80) != 0;
            int opcode = first & 0x0f;
            long length = second & 0x7f;
            if (length == 126) length = ((long) readByte(input) << 8) | readByte(input);
            else if (length == 127) {
                length = 0;
                for (int count = 0; count < 8; count++) length = (length << 8) | readByte(input);
            }
            if (length < 0 || length > WireCodec.MAX_FRAME_BYTES) throw new IOException("Server frame too large");
            if (opcode >= 8 && (!fin || length > 125)) throw new IOException("Invalid control frame");
            byte[] payload = new byte[(int) length];
            readFully(input, payload);
            if (opcode == 8) return;
            if (opcode == 9) { writeFrame(opened.getOutputStream(), 10, payload); continue; }
            if (opcode == 10) continue;
            if (opcode == 1) {
                if (fragments != null) throw new IOException("Overlapping WebSocket messages");
                fragments = new ByteArrayOutputStream();
            } else if (opcode != 0 || fragments == null) throw new IOException("Unsupported WebSocket frame");
            if (fragments.size() + payload.length > WireCodec.MAX_FRAME_BYTES)
                throw new IOException("WebSocket message too large");
            fragments.write(payload);
            if (fin) {
                publish(WireCodec.decodeUpdatePayload(fragments.toByteArray()));
                fragments = null;
            }
        }
    }

    private static int readByte(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) throw new EOFException("Truncated WebSocket frame");
        return value;
    }

    private static void readFully(InputStream input, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            int count = input.read(bytes, offset, bytes.length - offset);
            if (count < 0) throw new EOFException("Truncated WebSocket frame");
            offset += count;
        }
    }

    private void writeFrame(OutputStream output, int opcode, byte[] payload) throws IOException {
        if (payload.length > WireCodec.MAX_FRAME_BYTES) throw new IOException("WebSocket frame too large");
        byte[] mask = new byte[4]; RANDOM.nextBytes(mask);
        synchronized (writeLock) {
            output.write(0x80 | opcode);
            if (payload.length < 126) output.write(0x80 | payload.length);
            else if (payload.length <= 65535) {
                output.write(0x80 | 126); output.write((payload.length >>> 8) & 0xff); output.write(payload.length & 0xff);
            } else {
                output.write(0x80 | 127);
                for (int shift = 56; shift >= 0; shift -= 8) output.write((int) (((long) payload.length >>> shift) & 0xff));
            }
            output.write(mask);
            for (int index = 0; index < payload.length; index++) output.write(payload[index] ^ mask[index & 3]);
            output.flush();
        }
    }

    private void publish(NetworkUpdate update) {
        dispatch(() -> {
            switch (update.getType()) {
                case CONNECTED: connectedUpdate = update; break;
                case ROOM_STATE:
                    roomUpdate = update;
                    if (update.getRoomState().getPhase() == RoomState.Phase.CLOSED
                            || (update.getRoomState().getPhase() == RoomState.Phase.WAITING
                            && startUpdate != null && update.getRoomVersion() > startUpdate.getRoomVersion())) {
                        matchId = null; startUpdate = null; snapshotUpdate = null;
                    }
                    break;
                case MATCH_STARTED: matchId = update.getMatchId(); startUpdate = update; snapshotUpdate = null; break;
                case SNAPSHOT: snapshotUpdate = update; break;
                default: break;
            }
            for (Observer observer : new ArrayList<Observer>(observers)) observer.deliver(update);
        });
    }

    private void dispatch(Runnable action) {
        if (closed.get()) return;
        try { callbacks.execute(action); }
        catch (RejectedExecutionException overflow) { terminate("RECEIVE_QUEUE_FULL", false); }
    }

    private void terminate(String reason, boolean connectionFailure) {
        if (!closed.compareAndSet(false, true)) return;
        connected = false;
        Socket current = socket;
        if (current != null) try { current.close(); } catch (IOException ignored) { }
        Thread activeWriter = writer;
        if (activeWriter != null) activeWriter.interrupt();
        outgoing.clear();
        Runnable terminal = () -> {
            NetworkUpdate update = connectionFailure ? NetworkUpdate.connectionFailed(reason) : NetworkUpdate.closed(reason);
            for (Observer observer : new ArrayList<Observer>(observers)) observer.deliver(update);
            observers.clear();
        };
        try { callbacks.execute(terminal); }
        catch (RejectedExecutionException overflow) {
            callbacks.getQueue().poll();
            try { callbacks.execute(terminal); } catch (RejectedExecutionException ignored) { }
        }
        callbacks.shutdown();
    }

    private void requireOpen() { if (closed.get()) throw new IllegalStateException("CLOSED"); }
    @Override public void close() { terminate("CLIENT_CLOSED", false); }
    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name); thread.setDaemon(true); return thread;
    }

    private static final class Observer {
        private final NetworkListener listener;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private Observer(NetworkListener listener) { this.listener = listener; }
        private void deliver(NetworkUpdate update) {
            if (!active.get()) return;
            try { listener.onUpdate(update); }
            catch (RuntimeException failure) {
                java.util.logging.Logger.getLogger(WebSocketNetworkClient.class.getName())
                        .log(java.util.logging.Level.WARNING, "Network listener failed", failure);
            }
        }
    }
}
