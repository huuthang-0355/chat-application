package network.multiClient;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringBufferInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import network.protocol.Message;
import network.protocol.MessageType;
import server.db.GroupDAO;
import server.db.MessageDAO;
import server.db.UserDAO;
import server.service.AuthService;
import server.service.GroupService;
import server.session.SessionManager;

// implement Runnable to be able to put in ExecutorServic
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private MultiChatServer server; // reference to server to use broadcast() function
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    private boolean authenticated = false;
    private AuthService authService = new AuthService();

    private int userId;
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();

    // group
    private GroupService groupService = new GroupService();
    private GroupDAO groupDAO = new GroupDAO();

    private final String SERVER_STORAGE = "server_storage";

    public ClientHandler(Socket socket, MultiChatServer server) {
        this.clientSocket = socket;
        this.server = server;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("[ERROR]: Cannot initialize I/O stream for client");
        }

    }

    private void handleRegister(Message msg) {
        String usernameAttempt = msg.getSender();
        String password = msg.getContent();

        String result = authService.register(usernameAttempt, password);

        if (result.equals("OK")) {
            // check whether this user has logged in
            SessionManager sessionManager = server.getSessionManager();
            if (sessionManager.isOnline(usernameAttempt)) {
                Message failMessage = new Message(
                        MessageType.LOGIN_FAIL,
                        "SYSTEM",
                        usernameAttempt,
                        "Already logged in from another clinet");

                send(failMessage);
                return;
            }

            // save username
            this.username = usernameAttempt;
            this.userId = userDAO.findUserByUsername(this.username);

            // sever log
            System.out.println("[SERVER-LOG]: " + this.username + " has connected.");

            // automatically login when registering successfully
            this.authenticated = true;
            sessionManager.addSession(this.username, this);
            Message okMessage = new Message(
                    MessageType.LOGIN_OK,
                    "SYSTEM",
                    this.username,
                    "Registration successfully! Welcome, " + this.username);
            send(okMessage);

            Message systemMsg = new Message(
                    MessageType.MSG,
                    "SYSTEM",
                    "ALL",
                    this.username + " joined the chat.");

            server.broadcast(systemMsg, this);

            // broadcast updated user list
            server.broadcastUserList();

            // send this user's group list
            String groupList = groupService.getUserGroupList(this.userId);
            Message groupListMsg = new Message(MessageType.GROUP_LIST, "SYSTEM", this.username, groupList);
            this.send(groupListMsg);
        } else {
            // username already taken
            Message failMessage = new Message(
                    MessageType.LOGIN_FAIL,
                    "SYSTEM",
                    usernameAttempt,
                    result);

            send(failMessage);
        }
    }

    private void handleLogin(Message msg) {
        String usernameAttempt = msg.getSender();
        String password = msg.getContent();

        String result = authService.login(usernameAttempt, password);

        if (result.equals("OK")) {
            // check whether this user has logged in
            SessionManager sessionManager = server.getSessionManager();
            if (sessionManager.isOnline(usernameAttempt)) {
                Message failMessage = new Message(
                        MessageType.LOGIN_FAIL,
                        "SYSTEM",
                        usernameAttempt,
                        "Already logged in from another clinet");

                send(failMessage);
                return;
            }

            // save username
            this.username = usernameAttempt;
            this.userId = userDAO.findUserByUsername(this.username);

            // sever log
            System.out.println("[SERVER-LOG]: " + this.username + " has connected.");

            // save db for loggin
            this.authenticated = true;
            sessionManager.addSession(this.username, this);
            Message okMessage = new Message(
                    MessageType.LOGIN_OK,
                    "SYSTEM",
                    this.username,
                    "Registration successfully! Welcome, " + this.username);
            send(okMessage);

            Message systemMsg = new Message(
                    MessageType.MSG,
                    "SYSTEM",
                    "ALL",
                    this.username + " joined the chat.");

            server.broadcast(systemMsg, this);

            // broadcast updated user list
            server.broadcastUserList();

            // send this user's group list
            String groupList = groupService.getUserGroupList(this.userId);
            Message groupListMsg = new Message(MessageType.GROUP_LIST, "SYSTEM", this.username, groupList);
            this.send(groupListMsg);
        } else {
            // username already taken
            Message failMessage = new Message(
                    MessageType.LOGIN_FAIL,
                    "SYSTEM",
                    usernameAttempt,
                    result);

            send(failMessage);
        }
    }

    @Override
    public void run() {
        try {

            Message message;

            while ((message = (Message) in.readObject()) != null) {

                // 2. ignore if msg is invalid type
                if (message == null)
                    continue;

                // AUTHENTICATION GATE
                if (!authenticated) {
                    // only allow LOGIN and REGISTER
                    if (message.getType() == MessageType.REGISTER)
                        handleRegister(message);
                    else if (message.getType() == MessageType.LOGIN)
                        handleLogin(message);
                    else {
                        Message errorMsg = new Message(MessageType.ERROR, "SYSTEM", this.username,
                                "Please login first.");

                        send(message);
                    }

                    continue; // do NOT fall throguh to the switch below.
                }

                // AUTHENTICATED
                // 3. route msg basing on TYPE
                switch (message.getType()) {
                    case MSG:

                        // save message first to obtain auto-generated ID
                        int msgId = messageDAO.saveMessage(userId, null, message.getContent());

                        if (msgId != -1)
                            message.setMessageId(msgId);

                        // broadcast the message (carrying its ID) to everyone
                        server.broadcast(message, this);
                        break;

                    case PRIVATE:
                        String targetUser = message.getTarget();
                        ClientHandler targetHandler = server.getClientHandlerByUsername(targetUser);

                        if (targetHandler != null) {
                            // send raw data to target user
                            targetHandler.send(message);

                            // save private message into db (with receiver_id)
                            messageDAO.saveMessage(this.userId, targetHandler.getUserId(), message.getContent());
                        } else {
                            Message errMsg = new Message(MessageType.ERROR, "SYSTEM", username,
                                    "User " + targetUser + " is offline or not found.");
                            this.send(errMsg);
                        }

                        break;

                    case CREATE_GROUP:
                        String groupName = message.getContent();
                        String result = groupService.createGroup(groupName, this.userId);

                        if (result.startsWith("OK:")) {
                            int newGroupId = Integer.parseInt(result.split(":")[1]); // response is OK:groupdId

                            // send back user's updated group list
                            String groupList = groupService.getUserGroupList(this.userId);
                            Message listMsg = new Message(MessageType.GROUP_LIST, "SYSTEM", this.username, groupList);

                            this.send(listMsg);

                            // log
                            System.out.println("[SERVER-LOG]: " + username + " created group '" + groupName + "'");

                        } else {
                            Message errMsg = new Message(MessageType.ERROR, "SYSTEM", this.username, result);
                            this.send(errMsg);
                        }
                        break;

                    case JOIN_GROUP:
                        int joinGroupId = Integer.parseInt(message.getContent());

                        String joinResult = groupService.joinGroup(joinGroupId, this.userId);

                        if (joinResult.equals("OK")) {
                            String groupList = groupService.getUserGroupList(this.userId);
                            Message listMsg = new Message(MessageType.GROUP_LIST, "SYSTEM", this.username, groupList);

                            this.send(listMsg);
                        } else {
                            Message err = new Message(MessageType.ERROR, "SYSTEM", this.username, joinResult);

                            this.send(err);
                        }

                        break;

                    case LEAVE_GROUP:
                        int leaveGroupId = Integer.parseInt(message.getContent());

                        // notify remaining members before leaving
                        Message leaveMsg = new Message(MessageType.GROUP_MSG, "SYSTEM", String.valueOf(leaveGroupId),
                                this.username + " has left the group.");

                        server.sendToGroupMembers(leaveGroupId, leaveMsg);

                        String leaveResult = groupService.leaveGroup(leaveGroupId, this.userId);

                        if (leaveResult.equals("OK")) {
                            String groupList = groupService.getUserGroupList(this.userId);
                            Message listMsg = new Message(MessageType.GROUP_LIST, "SYSTEM", this.username, groupList);

                            this.send(listMsg);
                        } else {
                            Message errMsg = new Message(MessageType.ERROR, "SYSTEM", this.username, leaveResult);

                            this.send(errMsg);
                        }
                        break;

                    case GROUP_MSG:
                        int targetGroupId = Integer.parseInt(message.getTarget());

                        // only members can send to the group
                        if (!groupService.isMember(targetGroupId, this.userId)) {
                            Message err = new Message(MessageType.ERROR, "SYSTEM", this.username,
                                    "You are not a member of group " + targetGroupId);

                            this.send(err);
                            break;
                        }

                        // save group message first
                        int gMsgId = groupDAO.saveGroupMessage(targetGroupId, this.userId, message.getContent());

                        if (gMsgId != -1)
                            message.setMessageId(gMsgId);

                        // deliver message (carrying its ID) to all online members in group
                        server.sendToGroupMembers(targetGroupId, message);

                        break;

                    case FILE_UPLOAD:
                        // generate unique ID and save
                        String fileId = UUID.randomUUID().toString();
                        String filename = message.getFilename();
                        byte[] fileData = message.getFileData();

                        try {
                            File storageDir = new File(SERVER_STORAGE);
                            if (!storageDir.exists())
                                storageDir.mkdir();

                            File dest = new File(storageDir, fileId + "_" + filename);
                            Files.write(dest.toPath(), fileData);

                            // broadcast notificatio to clients (no bytes)
                            Message notifyMsg = new Message(MessageType.FILE_NOTIFY, this.username, message.getTarget(),
                                    filename, fileId, null);

                            if (message.getTarget().equals("ALL"))
                                server.broadcast(notifyMsg, this);
                            else if (message.getTarget().matches("\\d+")) {
                                int groupId = Integer.parseInt(message.getTarget());
                                server.sendToGroupMembers(groupId, notifyMsg);
                            }

                        } catch (Exception e) {
                            this.send(new Message(MessageType.ERROR, "SYSTEM", this.username,
                                    "Failed to save file on server.", null, null));
                        }
                        break;

                    case FILE_REQ: // client wants to downlaod a file
                        String requestedFileId = message.getFileId();
                        String requestedFilename = message.getFilename();

                        try {
                            File src = new File(SERVER_STORAGE, requestedFileId + "_" + requestedFilename);
                            if (src.exists()) {
                                byte[] bytes = Files.readAllBytes(src.toPath());

                                Message downloadMsg = new Message(MessageType.FILE_DOWNLOAD, "SYSTEM", this.username,
                                        requestedFilename, requestedFileId, bytes);

                                this.send(downloadMsg);
                            } else {
                                this.send(new Message(MessageType.ERROR, "SYSTEM", this.username,
                                        "File not found on server"));
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;

                    case FETCH_HISTORY:
                        // assume 'content' holds the offset -> "0" for the first 50 msgs
                        int offset = Integer.parseInt(message.getContent());
                        int limit = 50;

                        List<Message> history;

                        if (message.getTarget().equals("ALL")) {
                            // public chat
                            history = messageDAO.getPublicHistory(this.userId, limit, offset);
                        } else if (message.getTarget().matches("\\d+")) {
                            // target is a group ID
                            int groupId = Integer.parseInt(message.getTarget());
                            history = groupDAO.getGroupHistory(groupId, this.userId, limit, offset);
                        } else {
                            // target is a username (private chat)
                            int targetId = userDAO.findUserByUsername(message.getTarget());
                            history = messageDAO.getHistory(this.userId, targetId, limit, offset);
                        }

                        // reverse the list so chronological order is correct in the UI
                        Collections.reverse(history);

                        // send back to client
                        Message historyMsg = new Message(MessageType.HISTORY_RESPONSE, "SERVER", message.getTarget(),
                                history);

                        this.send(historyMsg);
                        break;

                    case DELETE_MSG:
                        int delMsgId = message.getMessageId();
                        String target = message.getTarget();
                        boolean success = false;

                        if (target != null && target.matches("\\d+")) {
                            // a group msg deletion
                            int groupId = Integer.parseInt(target);
                            success = groupDAO.deleteGroupMessage(delMsgId, this.userId, groupId);
                        } else {
                            // a public / private msg deletion
                            success = messageDAO.deleteMessage(delMsgId, this.userId);
                        }

                        if (success) {
                            Message deleteBroadcast = new Message(MessageType.DELETE_MSG,
                                    this.username, message.getTarget(), "");
                            deleteBroadcast.setMessageId(delMsgId);

                            // broadcast to all clients
                            server.broadcast(deleteBroadcast, null);

                        }
                        break;

                    case DELETE_CONVERSATION:
                        String clearTarget = message.getTarget();
                        int rowsDeleted = 0;

                        if (clearTarget.equals("ALL")) {
                            rowsDeleted = messageDAO.recordClearHistory(this.userId, "PUBLIC", "ALL");
                        } else if (clearTarget.matches("\\d+")) {
                            rowsDeleted = messageDAO.recordClearHistory(this.userId, "GROUP", clearTarget);
                        }

                        // send ACK back to the requester only
                        Message ackMsg = new Message(MessageType.DELETE_CONVERSATION, "SERVER", clearTarget,
                                String.valueOf(rowsDeleted));

                        this.send(ackMsg);

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
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            // code block for Clean Up in order to avoid MEMORY LEAK
            server.removeClient(this); // remove client from the list of server

            // remove user from session manager
            if (authenticated && username != null) {
                server.getSessionManager().removeSession(username);

                // informing user has left the room
                Message leaveMsg = new Message(MessageType.MSG, "SYSTEM", "ALL", this.username + " has left the room.");

                server.broadcast(leaveMsg, this);

                // updated user list (remove 1 user)
                server.broadcastUserList();
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
    public synchronized void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

}
