package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.protocol.Message;
import common.protocol.MessageType;
import server.db.GroupDAO;
import server.db.UserDAO;
import server.session.SessionManager;

public class MultiChatServer {

    private int port;

    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private ExecutorService executor = Executors.newCachedThreadPool();

    private SessionManager sessionManager = new SessionManager();

    private GroupDAO groupDAO = new GroupDAO();
    private UserDAO userDAO = new UserDAO();

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private server.view.ServerListener listener;

    public void setListener(server.view.ServerListener listener) {
        this.listener = listener;
    }

    public server.view.ServerListener getListener() {
        return this.listener;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void log(String message) {
        System.out.println(message);
        if (listener != null) {
            listener.onLogMessage(message);
        }
    }

    public MultiChatServer(int port) {
        this.port = port;
    }

    public void startServer() {
        this.isRunning = true;
        try {
            serverSocket = new ServerSocket(port);
            log("[SYSTEM]: Server is running on port " + port);
            if (listener != null) {
                listener.onServerStarted();
            }

            // infinite loop for only waiting users
            while (isRunning) {
                Socket socket = serverSocket.accept();
                
                if (!isRunning) {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    break;
                }

                // new user enter --> create handler
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);

                // submit task for executor to handle
                executor.submit(clientHandler);

                log("[SYSTEM]: New client connected. Serving " + clients.size() + " clients");
            }
        } catch (IOException e) {
            if (isRunning) {
                log("[SERVER-ERROR]: " + e.getMessage());
            } else {
                log("[SYSTEM]: Server stopped gracefully.");
            }
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("[SERVER-ERROR] Error closing server socket: " + e.getMessage());
        }
        executor.shutdown();
        if (listener != null) {
            listener.onServerStopped();
        }
    }

    public void stopServer() {
        this.isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("[SERVER-ERROR] Error closing server socket: " + e.getMessage());
        }
        // close all active connections
        for (ClientHandler client : clients) {
            client.closeConnection();
        }
        clients.clear();
        executor.shutdownNow();
        executor = Executors.newCachedThreadPool(); // recreate for potential next start
    }

    // receive msg and sender, send msg to other clients (not sender)
    public void broadcast(Message msg, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.isAuthenticated())
                client.send(msg);
        }
    }

    public void broadcastUserList() {
        List<String> onlineUsernameList = sessionManager.getOnlineUsernames();
        List<String> list = new java.util.ArrayList<>();
        for (String uname : onlineUsernameList) {
            ClientHandler handler = sessionManager.getHandler(uname);
            if (handler != null) {
                list.add(handler.getDisplayName() + ":" + uname);
            } else {
                list.add(uname + ":" + uname);
            }
        }

        String userListStr = String.join(",", list);

        Message userListMsg = new Message(MessageType.USER_LIST, "SYSTEM", "ALL", userListStr);

        broadcast(userListMsg, null);
    }

    // remove client func, which will be called in finally block of ClientHandler
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("[SYSTEM]: Removed 1 client. Remaining: " + clients.size());
    }

    public ClientHandler getClientHandlerByUsername(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username))
                return client;
        }

        return null;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    // used for sending msg to a specific group
    public void sendToGroupMembers(int groupId, Message msg) {
        // find all members in a group
        List<Integer> memberIdList = groupDAO.getMemberIds(groupId);

        for (int memberId : memberIdList) {
            // get username for each memberId
            String memberUsername = userDAO.getUsernameById(memberId);

            if (memberUsername == null)
                continue;

            // get handler of online usernames (members)
            ClientHandler handler = sessionManager.getHandler(memberUsername);

            if (handler != null)
                handler.send(msg);
        }
    }
}
