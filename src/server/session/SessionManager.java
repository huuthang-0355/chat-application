package server.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.multiClient.ClientHandler;

public class SessionManager {
    private final Map<String, ClientHandler> onlineSessions = new HashMap<>();

    public synchronized boolean isOnline(String username) {
        return onlineSessions.containsKey(username);
    }

    public synchronized void addSession(String username, ClientHandler handler) {
        onlineSessions.put(username, handler);
    }

    public synchronized void removeSession(String username) {
        onlineSessions.remove(username);
    }

    public synchronized ClientHandler getHandler(String username) {
        return onlineSessions.get(username);
    }

    public synchronized List<String> getOnlineUsernames() {

        return new ArrayList<String>(onlineSessions.keySet());
    }
}
