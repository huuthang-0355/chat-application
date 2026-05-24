package client.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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

    private final long FILE_MAX_SIZE = 5 * 1024 * 1024; // size limit (5MB)

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

    public void loadHistory(String target, int offset) {
        // target = "ALL" or "groupId", or "username"
        Message req = new Message(MessageType.FETCH_HISTORY, username, target, String.valueOf(offset));

        networkService.send(req);
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

                    // load history for public chat on successful login
                    loadHistory("ALL", 0);
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
                String text;
                boolean isMe = sender.equals(this.username);
                String displaySender = isMe ? "YOU" : sender;

                if (isMe && msg.getMessageId() > 0) {
                    // Your message with a valid DB id - show delete button
                    text = String.format("<div id='msg-%d'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                            msg.getMessageId(), msg.getContent(), msg.getMessageId());
                } else if (msg.getMessageId() > 0) {
                    // Someone else's message with a valid DB id
                    text = String.format("<div id='msg-%d'><b>[%s]</b>: %s</div>",
                            msg.getMessageId(), displaySender, msg.getContent());
                } else {
                    // System message or no ID — no delete button, still show YOU if applicable
                    text = String.format("<b>[%s]</b>: %s", displaySender, msg.getContent());
                }

                chatView.displayMessage(text);
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
                String groupText;
                boolean _isMe = _sender.equals(this.username);
                String _displaySender = _isMe ? "YOU" : _sender;

                if (_isMe && msg.getMessageId() > 0) {
                    groupText = String.format("<div id='msg-%d'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                            msg.getMessageId(), msg.getContent(), msg.getMessageId());
                } else if (msg.getMessageId() > 0) {
                    groupText = String.format("<div id='msg-%d'><b>[%s]</b>: %s</div>",
                            msg.getMessageId(), _displaySender, msg.getContent());
                } else {
                    groupText = String.format("<b>[%s]</b>: %s", _displaySender, msg.getContent());
                }

                chatView.displayGroupMessage(groupId, groupText);
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

            case FILE_NOTIFY:

                String sender_ = msg.getSender();
                String fname = msg.getFilename();
                String fid = msg.getFileId();

                // build clickable HTML link
                // format in hrefValue 'fid:fname', 'f8293:report.pdf'
                String htmlNotification = String.format(
                        "<b>[%s]</b>: 📎 Shared a file '%s' - <a href='%s:%s'>[Download]</a>", sender_, fname, fid,
                        fname);

                String target = msg.getTarget();
                if (target.equals("ALL")) {
                    chatView.displayMessage(htmlNotification);
                } else if (target.matches("\\d+")) {
                    chatView.displayGroupMessage(Integer.parseInt(target), htmlNotification);
                }
                break;

            case FILE_DOWNLOAD:
                String downloadFilename = msg.getFilename();
                byte[] downloadBytes = msg.getFileData();

                chatView.promptFileSave(downloadFilename, downloadBytes);

                break;

            case HISTORY_RESPONSE:
                List<Message> pastMessages = msg.getHistoryList();
                if (pastMessages == null)
                    break;

                for (Message pastMsg : pastMessages) {
                    String content = pastMsg.getContent();
                    String histText;

                    if (content.equals("[This message was deleted]")) {

                        String dispSender = pastMsg.getSender().equals(this.username) ? "YOU" : pastMsg.getSender();

                        histText = String.format("<div id='msg-%d'><b>[%s]</b>: <i>%s</i></div>",
                                pastMsg.getMessageId(), dispSender, content);
                    } else {
                        // only show delete button if current user is the sender
                        if (pastMsg.getSender().equals(this.username)) {
                            histText = String.format(
                                    "<div id='msg-%d'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                                    pastMsg.getMessageId(), content, pastMsg.getMessageId());
                        } else {
                            histText = String.format("<div id='msg-%d'><b>[%s]</b>: %s</div>",
                                    pastMsg.getMessageId(), pastMsg.getSender(), content);
                        }
                    }

                    if (msg.getTarget().equals("ALL"))
                        chatView.displayMessage(histText);
                    else if (msg.getTarget().matches("\\d+"))
                        chatView.displayGroupMessage(Integer.parseInt(msg.getTarget()), histText);

                }
                break;

            case DELETE_MSG:
                if (chatView != null) {
                    String dispSender = msg.getSender().equals(this.username) ? "YOU" : msg.getSender();

                    chatView.updateMessageDeleted(msg.getMessageId(), dispSender);
                }

                break;

            case DELETE_CONVERSATION:
                // clear the pane silently
                if (chatView != null) {
                    chatView.clearChatArea(msg.getTarget());
                }
                break;

            case ERROR:
                chatView.displayMessage("[Error]: " + msg.getContent());
                break;

            default:
                break;
        }
    }

    public void deleteMessage(int messageId, String target) {
        Message req = new Message(MessageType.DELETE_MSG, username, target,
                "");
        req.setMessageId(messageId); // set the ID directly using your setter
        networkService.send(req);
    }

    public void disconnect() {
        if (networkService != null) {
            networkService.send(new Message(MessageType.LOGOUT, username, "ALL", ""));
            networkService.disconnect();
        }

        System.exit(0);
    }

    // send file function
    public void sendFile(String target) {
        // open file chooser
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a file to send");
        int result = chooser.showOpenDialog(null);

        if (result != JFileChooser.APPROVE_OPTION)
            return;

        File selectedFile = chooser.getSelectedFile();

        // check file size limit
        if (selectedFile.length() > FILE_MAX_SIZE) {
            JOptionPane.showMessageDialog(null,
                    "File too large. Maximum size is 5MB",
                    "File Error", JOptionPane.ERROR_MESSAGE);

            return;
        }

        // read and send in background thread - not block EDT UI
        new Thread(() -> {

            try {
                // read bytes
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());

                // send FILE_UPLOAD
                Message uploadMsg = new Message(MessageType.FILE_UPLOAD, this.username, target, selectedFile.getName(),
                        null, fileBytes);
                // send object
                networkService.send(uploadMsg);

                // notify UI in the correct tab
                SwingUtilities.invokeLater(() -> {

                    String text = "[YOU]: 🔗 Sent file: " + selectedFile.getName();

                    if (target.equals("ALL"))
                        chatView.displayMessage(text);
                    else if (target.matches("\\d+"))
                        chatView.displayGroupMessage(Integer.parseInt(target), text);

                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Failed to read file: " + ex.getMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE));
            }

        }).start();

    }

    /// send request to server for downloading file
    public void requestFileDownload(String hrefData) {
        // hrefData -> "f2312:report.pdf"
        String[] parts = hrefData.split(":");
        String fileId = parts[0];
        String filename = parts[1];

        // contains HTML request, no data bytes
        Message reqMsg = new Message(MessageType.FILE_REQ, this.username, "SERVER", filename, fileId, null);
        networkService.send(reqMsg);
    }
    // helper functions

    public void clearHistory(String target) {
        Message deleteMsg = new Message(MessageType.DELETE_CONVERSATION, username, target, "");

        networkService.send(deleteMsg);
    }

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
