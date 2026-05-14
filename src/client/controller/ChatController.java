package client.controller;

import java.io.IOError;
import java.io.IOException;

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

    // called by LoginView
    public void connect(String username, String host, int port, LoginView loginView) {
        try {
            this.username = username;
            // create a new thread to read msg
            networkService = new NetworkService(host, port, this);
            networkService.start();

            // send LOGIN package
            networkService.send(new Message(MessageType.LOGIN, username, "ALL", ""));

            // close login UI and open chat UI
            SwingUtilities.invokeLater(() -> {
                loginView.dispose();
                chatView = new ChatView(this);
                chatView.setVisible(true);
            });
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
        if (chatView == null)
            return; // guard: ChatView may not be open yet

        switch (msg.getType()) {
            case MSG:
                String sender = msg.getSender();
                if (msg.getSender().equals(this.username)) {
                    sender = "YOU";
                }

                chatView.displayMessage("[" + sender + "]: " + msg.getContent());
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
}
