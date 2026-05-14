package network.multiClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import network.protocol.Message;
import network.protocol.MessageParser;
import network.protocol.MessageType;
import server.db.MessageDAO;
import server.db.UserDAO;

// implement Runnable to be able to put in ExecutorServic
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private MultiChatServer server; // reference to server to use broadcast() function
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private int userId;
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket, MultiChatServer server) {
        this.clientSocket = socket;
        this.server = server;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("[ERROR]: Cannot initialize I/O stream for client");
        }

    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public void run() {
        try {

            String rawLine;

            while ((rawLine = in.readLine()) != null) {
                // 1. decode raw msg to Message object
                Message message = MessageParser.decode(rawLine);

                // 2. ignore if msg is invalid type
                if (message == null)
                    continue;

                // 3. route msg basing on TYPE
                switch (message.getType()) {
                    case LOGIN:
                        // save username from LOGIN package
                        this.username = message.getSender();

                        // check whether this username existed in db
                        int dbUserId = userDAO.findUserByUsername(username);
                        if (dbUserId == -1) { // not existing
                            userDAO.createUser(this.username, "nopassword");

                            dbUserId = userDAO.findUserByUsername(this.username); // re-find id in db after creating
                        }

                        this.userId = dbUserId;

                        System.out.println("[SERVER-LOG]: " + this.username + " has connected.");

                        // create MSG package so that system infomrs of all users.
                        Message sysMsg = new Message(MessageType.MSG, "SYSTEM", "ALL",
                                username + " joined in chat room.");

                        server.broadcast(MessageParser.encode(sysMsg), this);
                        break;

                    case MSG:
                        server.broadcast(rawLine, this);

                        // save message (all -> null in receiver)
                        messageDAO.saveMessage(userId, null, message.getContent());
                        break;

                    case PRIVATE:
                        String targetUser = message.getTarget();
                        ClientHandler targetHandler = server.getClientHandlerByUsername(targetUser);

                        if (targetHandler != null) {
                            // send raw data to target user
                            targetHandler.send(rawLine);

                            // save private message into db (with receiver_id)
                            messageDAO.saveMessage(this.userId, targetHandler.getUserId(), message.getContent());
                        } else {
                            Message errMsg = new Message(MessageType.ERROR, "SYSTEM", username,
                                    "User " + targetUser + " is offline or not found.");
                            this.send(MessageParser.encode(errMsg));
                        }

                        break;

                    case LOGOUT:
                        // stop the while loop and jump in finally block
                        return;
                    default:
                        break;
                }
            }

        } catch (SocketException e) {
            // catch errors when user suddenly close terminal or has wifi disconnection
            System.out.println("[SERVER-LOG]: " + this.username + " suddenly disconnected");
        } catch (IOException e) {
            System.out.println("Error disconnection from " + username);
        } finally {
            // code block for Clean Up in order to avoid MEMORY LEAK
            server.removeClient(this); // remove client from the list of server

            // create system package informing user has left the room
            if (this.username != null) {
                Message leaveMsg = new Message(MessageType.MSG, "SYSTEM", "ALL", this.username + " has left the room.");

                server.broadcast(MessageParser.encode(leaveMsg), this);
            }

            // close resource
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (clientSocket != null && !clientSocket.isClosed())
                    clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    // this func is used for MultiChatServer calling when it iterates the list and
    // want to send msg to this client
    // WHY SYNCHRONIZED: for example we have 3 clients: AN, BINH, CHI. If AN and
    // BINH send msg simultaneously
    // they will race the right to use the out -> race condition.
    public synchronized void send(String msg) {
        if (this.out != null)
            out.println(msg);
    }
}
