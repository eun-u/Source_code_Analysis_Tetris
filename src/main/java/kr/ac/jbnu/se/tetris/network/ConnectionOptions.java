package kr.ac.jbnu.se.tetris.network;

/** 실제 네트워크 클라이언트의 서버 접속 대상 */
public final class ConnectionOptions {
    private final String host;
    private final int port;

    public ConnectionOptions(String host, int port) {
        if (host == null || host.trim().isEmpty() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid connection options");
        }
        this.host = host.trim();
        this.port = port;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}
