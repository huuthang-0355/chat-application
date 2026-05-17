package network.multiClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.w3c.dom.UserDataHandler;

import network.protocol.Message;
import network.protocol.MessageParser;
import network.protocol.MessageType;
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

    public MultiChatServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SYSTEM]: Server is running on port " + port);

            // infinite loop for only waiting users
            while (true) {
                Socket socket = serverSocket.accept();

                // new user enter --> create handler
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);

                // submit task for executor to handle
                executor.submit(clientHandler);

                System.out
                        .println("[SYSTEM]: New client connected. Serving " + clients.size() + " clients");
            }
        } catch (IOException e) {
            System.out.println("[SEVER-ERROR]: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    // receive msg and sender, send msg to other clients (not sender)
    public void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.isAuthenticated())
                client.send(msg);
        }
    }

    public void broadcastUserList() {
        List<String> onlineUsernameList = sessionManager.getOnlineUsernames();

        String userListStr = String.join(",", onlineUsernameList);

        Message msg = new Message(MessageType.USER_LIST, "SYSTEM", "ALL", userListStr);

        String encodedString = MessageParser.encode(msg);

        broadcast(encodedString, null);
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
    public void sendToGroupMembers(int groupId, String encodedMsg) {
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
                handler.send(encodedMsg);
        }
    }
}
