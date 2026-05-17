package client.controller;

import java.io.IOException;
import java.util.List;

import javax.swing.SwingUtilities;

import client.network.NetworkService;
import client.view.ChatView;
import client.view.LoginView;
import network.protocol.Message;
import network.protocol.MessageType;

public class ChatController {

    private ChatView chatView;
    private NetworkService networkService;
    private String username;
    private LoginView loginView;

    private String pendingUserList = null; // online users buffer for initial login

    private String pendingGroupList = null; // group list users belongs to for initial login

    // called by LoginView
    public void connect(String username, String password, boolean isRegister, String host, int port,
            LoginView loginView) {
        try {

            // CLOSE previous connection before openning a new one
            if (networkService != null) {
                networkService.disconnect();
                networkService = null;
            }

            this.username = username;
            this.loginView = loginView;

            // create a new thread to read msg
            networkService = new NetworkService(host, port, this);
            networkService.start();

            // send either REGISTER or LOGIN
            MessageType authType = isRegister ? MessageType.REGISTER : MessageType.LOGIN;
            networkService.send(new Message(authType, username, "ALL", password));

        } catch (IOException e) {
            loginView.setStatus("Connection error: " + e.getMessage());
        }
    }

    // called by ChatView when user clicks 'Send'
    public void sendMessages(String text) {
        if (text == null || text.isEmpty())
            return;

        networkService.send(new Message(MessageType.MSG, username, "ALL", text));
    }

    // called by NetworkService (background thread)
    public void onMessageReceived(Message msg) {

        switch (msg.getType()) {
            case LOGIN_OK: // auth succeed -> open ChatView
                this.username = msg.getTarget();

                SwingUtilities.invokeLater(() -> {
                    loginView.dispose();

                    chatView = new ChatView(this);
                    chatView.setVisible(true);
                    chatView.displayMessage("[SYSTEM]: " + msg.getContent());

                    // aplly buffer user list if it arrvied before chatView was ready
                    if (pendingUserList != null) {
                        chatView.updateUserList(pendingUserList.split(","));
                        pendingUserList = null;
                    }

                    if (pendingGroupList != null) {
                        chatView.updateGroupList(pendingGroupList);
                        pendingGroupList = null;
                    }
                });
                break;

            case LOGIN_FAIL:
                // auth failed -> show error in LoginView
                loginView.setStatus(msg.getContent());

                break;

            case MSG:
                if (chatView == null)
                    return; // guard: ChatView may not be open yet
                String sender = msg.getSender();
                if (msg.getSender().equals(this.username)) {
                    sender = "YOU";
                }

                chatView.displayMessage("[" + sender + "]: " + msg.getContent());
                break;

            case USER_LIST:
                // content = "alice,peter,roger"
                String[] onlineUsers = msg.getContent().split(",");

                if (chatView != null) {
                    // handle UI
                    chatView.updateUserList(onlineUsers);
                } else {
                    pendingUserList = msg.getContent();
                }

                break;

            case GROUP_MSG:
                if (chatView == null)
                    return;

                // target = groupId, sender = username, content = msg text
                int groupId = Integer.parseInt(msg.getTarget());
                String _sender = msg.getSender();

                if (_sender.equals(this.username))
                    _sender = "YOU";

                String display = "[" + _sender + "]: " + msg.getContent();

                chatView.displayGroupMessage(groupId, display);
                break;

            case GROUP_LIST:
                // content = "team-a:3,study:7" or ""
                if (chatView != null) {
                    chatView.updateGroupList(msg.getContent());
                } else {
                    pendingGroupList = msg.getContent();
                }

                break;

            case CREATE_GROUP:
                // sever does NOT send CREATE_GROUP back - it send GROUP_LIST instead
                break;

            case PRIVATE:
                chatView.displayMessage("[Private from " + msg.getSender() + "]: " + msg.getContent());
                break;

            case ERROR:
                chatView.displayMessage("[Error]: " + msg.getContent());
                break;

            default:
                break;
        }
    }

    public void disconnect() {
        if (networkService != null) {
            networkService.send(new Message(MessageType.LOGOUT, username, "ALL", ""));
            networkService.disconnect();
        }

        System.exit(0);
    }

    // helper function

    public void createGroup(String groupName) {
        networkService.send(new Message(MessageType.CREATE_GROUP, this.username, "ALL", groupName));
    }

    // called by ChatView when user clicks Join
    public void joinGroup(int groupId) {
        networkService.send(new Message(MessageType.JOIN_GROUP, username, "ALL", String.valueOf(groupId)));
    }

    // called by ChatView when user clicks Leave
    public void leaveGroup(int groupId) {
        networkService.send(new Message(MessageType.LEAVE_GROUP, username, "ALL", String.valueOf(groupId)));
    }

    // called by ChatView when user sends a group message
    public void sendGroupMessage(int groupId, String text) {
        networkService.send(new Message(MessageType.GROUP_MSG, username, String.valueOf(groupId), text));
    }
}
