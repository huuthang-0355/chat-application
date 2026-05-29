package server.view;

public interface ServerListener {
    void onServerStarted();
    void onServerStopped();
    void onClientConnected(String username, String displayName, String ipAddress);
    void onClientDisconnected(String username);
    void onLogMessage(String message);
}
