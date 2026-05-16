package network.multiClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import server.session.SessionManager;

public class MultiChatServer {

    private int port;

    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private ExecutorService executor = Executors.newCachedThreadPool();

    private SessionManager sessionManager = new SessionManager();

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

}
