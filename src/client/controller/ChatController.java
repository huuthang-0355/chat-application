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
import common.protocol.Message;
import common.protocol.MessageType;

public class ChatController {

    private ChatView chatView;
    private NetworkService networkService;
    private String username;
    private String displayName;
    private LoginView loginView;
    private String host;
    private int port;

    private String pendingUserList = null; // online users buffer for initial login

    private String pendingGroupList = null; // group list users belongs to for initial login

    public String getDisplayName() {
        return displayName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    // called by LoginView
    public void connect(String username, String password, String displayName, boolean isRegister, String host, int port,
            LoginView loginView) {
        try {

            // CLOSE previous connection before openning a new one
            if (networkService != null) {
                networkService.disconnect();
                networkService = null;
            }

            this.username = username;
            this.displayName = displayName;
            this.loginView = loginView;
            this.host = host;
            this.port = port;

            // create a new thread to read msg
            networkService = new NetworkService(host, port, this);
            networkService.start();

            // send either REGISTER or LOGIN
            MessageType authType = isRegister ? MessageType.REGISTER : MessageType.LOGIN;
            Message req = new Message(authType, username, "ALL", password);
            req.setDisplayName(displayName);
            networkService.send(req);

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
                this.displayName = msg.getDisplayName();

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
                String displaySender = isMe ? "YOU" : (msg.getDisplayName() != null ? msg.getDisplayName() : sender);
                String cleanContent = msg.getContent() != null ? msg.getContent().replace("\n", "<br/>") : "";

                if (isMe && msg.getMessageId() > 0) {
                    // Your message with a valid DB id - show delete button
                    text = String.format(
                            "<div id='msg-%d' align='right' style='text-align: right;'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                            msg.getMessageId(), cleanContent, msg.getMessageId());
                } else if (msg.getMessageId() > 0) {
                    // Someone else's message with a valid DB id
                    text = String.format(
                            "<div id='msg-%d' align='left' style='text-align: left;'><b>[%s]</b>: %s</div>",
                            msg.getMessageId(), displaySender, cleanContent);
                } else {
                    // System message or no ID — no delete button, still show YOU if applicable
                    text = String.format("<b>[%s]</b>: %s", displaySender, cleanContent);
                }

                String align = isMe ? "right" : "left";
                String timeStr = getSmartTimestamp(msg.getTimestamp());
                String timeDiv = String.format(
                        "<div align='%s' style='text-align: %s; margin-top: 2px;'><font color='#666666' size='2'><i>%s</i></font></div>",
                        align, align, timeStr);
                text += timeDiv;

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

            case GROUP_MEMBERS:
                String membersData = msg.getContent();
                String targetGroupId = msg.getTarget();
                if (chatView != null) {
                    chatView.showGroupMembers(targetGroupId, membersData);
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
                String _displaySender = _isMe ? "YOU" : (msg.getDisplayName() != null ? msg.getDisplayName() : _sender);
                String cleanGroupContent = msg.getContent() != null ? msg.getContent().replace("\n", "<br/>") : "";

                if (_isMe && msg.getMessageId() > 0) {
                    groupText = String.format(
                            "<div id='msg-%d' align='right' style='text-align: right;'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                            msg.getMessageId(), cleanGroupContent, msg.getMessageId());
                } else if (msg.getMessageId() > 0) {
                    groupText = String.format(
                            "<div id='msg-%d' align='left' style='text-align: left;'><b>[%s]</b>: %s</div>",
                            msg.getMessageId(), _displaySender, cleanGroupContent);
                } else {
                    groupText = String.format("<b>[%s]</b>: %s", _displaySender, cleanGroupContent);
                }

                String _align = _isMe ? "right" : "left";
                String _timeStr = getSmartTimestamp(msg.getTimestamp());
                String _timeDiv = String.format(
                        "<div align='%s' style='text-align: %s; margin-top: 2px;'><font color='#666666' size='2'><i>%s</i></font></div>",
                        _align, _align, _timeStr);
                groupText += _timeDiv;

                chatView.ensureTabOpen(String.valueOf(groupId));
                chatView.appendMessageToTab(String.valueOf(groupId), groupText);
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
                if (chatView == null)
                    return;
                String prvSender = msg.getSender();
                String prvText = msg.getContent();
                boolean isPrvMe = prvSender.equals(this.username);
                String dispPrvSender = isPrvMe ? "YOU"
                        : (msg.getDisplayName() != null ? msg.getDisplayName() : prvSender);
                String prvTarget = isPrvMe ? msg.getTarget() : prvSender; // where to show it
                String cleanPrvContent = prvText != null ? prvText.replace("\n", "<br/>") : "";

                // Auto open the tab
                chatView.ensureTabOpen(prvTarget);

                String formattedPrvText;
                if (isPrvMe && msg.getMessageId() > 0) {
                    formattedPrvText = String.format(
                            "<div id='msg-%d' align='right' style='text-align: right;'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                            msg.getMessageId(), cleanPrvContent, msg.getMessageId());
                } else if (msg.getMessageId() > 0) {
                    formattedPrvText = String.format(
                            "<div id='msg-%d' align='left' style='text-align: left;'><b>[%s]</b>: %s</div>",
                            msg.getMessageId(), dispPrvSender, cleanPrvContent);
                } else {
                    formattedPrvText = String.format("<b>[%s]</b>: %s", dispPrvSender, cleanPrvContent);
                }

                String prvAlign = isPrvMe ? "right" : "left";
                String prvTimeStr = getSmartTimestamp(msg.getTimestamp());
                String prvTimeDiv = String.format(
                        "<div align='%s' style='text-align: %s; margin-top: 2px;'><font color='#666666' size='2'><i>%s</i></font></div>",
                        prvAlign, prvAlign, prvTimeStr);
                formattedPrvText += prvTimeDiv;

                chatView.appendMessageToTab(prvTarget, formattedPrvText);
                break;

            case FILE_NOTIFY:
                String sender_ = msg.getSender();
                String fname = msg.getFilename();
                String fid = msg.getFileId();

                boolean isFileMe = sender_.equals(this.username);
                String fAlign = isFileMe ? "right" : "left";
                String dispFileSender = isFileMe ? "YOU"
                        : (msg.getDisplayName() != null ? msg.getDisplayName() : sender_);

                // build clickable HTML link with unique id to prevent duplicate display
                String htmlNotification = String.format(
                        "<div id='file-%s' align='%s' style='text-align: %s;'><b>[%s]</b>: \uD83D\uDCCE Shared a file '%s' - <a href='%s:%s'>[Download]</a></div>",
                        fid, fAlign, fAlign, dispFileSender, fname, fid, fname);

                String fTimeStr = getSmartTimestamp(msg.getTimestamp());
                String fTimeDiv = String.format(
                        "<div align='%s' style='text-align: %s; margin-top: 2px;'><font color='#666666' size='2'><i>%s</i></font></div>",
                        fAlign, fAlign, fTimeStr);
                htmlNotification += fTimeDiv;

                String target = msg.getTarget();
                if (target.equals("ALL")) {
                    chatView.displayMessage(htmlNotification);
                } else if (target.matches("\\d+")) {
                    chatView.ensureTabOpen(target);
                    chatView.appendMessageToTab(target, htmlNotification);
                } else {
                    String prvFileTarget = isFileMe ? target : sender_;
                    chatView.ensureTabOpen(prvFileTarget);
                    chatView.appendMessageToTab(prvFileTarget, htmlNotification);
                }
                break;

            case FILE_DOWNLOAD:
                String downloadFilename = msg.getFilename();
                byte[] downloadBytes = msg.getFileData();

                chatView.promptFileSave(downloadFilename, downloadBytes);

                break;

            case HISTORY_RESPONSE:
                List<Message> pastMessages = msg.getHistoryList();
                if (pastMessages == null) {
                    if (!msg.getTarget().equals("ALL")) {
                        chatView.setHistoryLoaded(msg.getTarget());
                    }
                    break;
                }

                java.util.Set<String> renderedIds = new java.util.HashSet<>();

                for (Message pastMsg : pastMessages) {
                    String content = pastMsg.getContent();
                    String histText;
                    boolean isPastMe = pastMsg.getSender().equals(this.username);
                    String dispSender = isPastMe ? "YOU"
                            : (pastMsg.getDisplayName() != null ? pastMsg.getDisplayName() : pastMsg.getSender());
                    String histAlign = isPastMe ? "right" : "left";
                    String cleanPastContent = content != null ? content.replace("\n", "<br/>") : "";
 
                    // Detect file messages: content stored as "📎 Shared a file '...' - <a
                    // href='fileId:filename'>..."
                    String fileIdFromHistory = null;
                    if (content != null && content.contains("Shared a file") && content.contains("<a href='")) {
                        // extract fileId from href='fileId:filename'
                        int hrefStart = content.indexOf("<a href='") + 9;
                        int hrefEnd = content.indexOf(":", hrefStart);
                        if (hrefEnd > hrefStart) {
                            fileIdFromHistory = content.substring(hrefStart, hrefEnd);
                        }
                    }
 
                    if (content.equals("[This message was deleted]")) {
                        histText = String.format(
                                "<div id='msg-%d' align='%s' style='text-align: %s;'><b>[%s]</b>: <i>%s</i></div>",
                                pastMsg.getMessageId(), histAlign, histAlign, dispSender, content);
                        renderedIds.add("msg-" + pastMsg.getMessageId());
                    } else if (fileIdFromHistory != null) {
                        // File message: use id='file-{fileId}' to match real-time FILE_NOTIFY element
                        // ID
                        if (isPastMe) {
                            histText = String.format(
                                    "<div id='file-%s' align='right' style='text-align: right;'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                                    fileIdFromHistory, cleanPastContent, pastMsg.getMessageId());
                        } else {
                            histText = String.format(
                                    "<div id='file-%s' align='left' style='text-align: left;'><b>[%s]</b>: %s</div>",
                                    fileIdFromHistory, dispSender, cleanPastContent);
                        }
                        renderedIds.add("file-" + fileIdFromHistory);
                    } else {
                        if (isPastMe) {
                            histText = String.format(
                                    "<div id='msg-%d' align='right' style='text-align: right;'><b>[YOU]</b>: %s <a href='del:%d'>[🗑️]</a></div>",
                                    pastMsg.getMessageId(), cleanPastContent, pastMsg.getMessageId());
                        } else {
                            histText = String.format(
                                    "<div id='msg-%d' align='left' style='text-align: left;'><b>[%s]</b>: %s</div>",
                                    pastMsg.getMessageId(), dispSender, cleanPastContent);
                        }
                        renderedIds.add("msg-" + pastMsg.getMessageId());
                    }
 
                    String histTimeStr = getSmartTimestamp(pastMsg.getTimestamp());
                    String histTimeDiv = String.format(
                            "<div align='%s' style='text-align: %s; margin-top: 2px;'><font color='#666666' size='2'><i>%s</i></font></div>",
                            histAlign, histAlign, histTimeStr);
                    histText += histTimeDiv;

                    if (msg.getTarget().equals("ALL")) {
                        chatView.displayMessage(histText);
                    } else {
                        chatView.appendHistoryToTab(msg.getTarget(), histText);
                    }
                }
                if (!msg.getTarget().equals("ALL")) {
                    chatView.setHistoryLoaded(msg.getTarget(), renderedIds);
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
                if (chatView != null) {
                    chatView.hideProgress();
                    chatView.displayMessage("[Error]: " + msg.getContent());
                }
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

    public String getUsername() {
        return this.username;
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

        // read and send in background thread - not block EDT UI
        new Thread(() -> {

            try {
                chatView.showProgress("Uploading file: " + selectedFile.getName() + "...");

                // read bytes
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());

                // send FILE_UPLOAD
                Message uploadMsg = new Message(MessageType.FILE_UPLOAD, this.username, target, selectedFile.getName(),
                        null, fileBytes);
                // send object
                networkService.send(uploadMsg);

                // No local redundant notification here as the server-confirmed FILE_NOTIFY will
                // handle it.
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Failed to read file: " + ex.getMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                chatView.hideProgress();
            }

        }).start();

    }

    /// send request to server for downloading file
    public void requestFileDownload(String hrefData) {
        // hrefData -> "f2312:report.pdf"
        String[] parts = hrefData.split(":");
        String fileId = parts[0];
        String filename = parts[1];

        chatView.showProgress("Downloading file: " + filename + "...");

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

    // called by ChatView when user sends a private message
    public void sendPrivateMessage(String targetUsername, String text) {
        networkService.send(new Message(MessageType.PRIVATE, username, targetUsername, text));
    }

    // called by ChatView to get group members
    public void requestGroupMembers(int groupId) {
        Message req = new Message(MessageType.GROUP_MEMBERS, this.username, String.valueOf(groupId), "");
        networkService.send(req);
    }

    private String getSmartTimestamp(String dbTimestampStr) {
        try {
            java.util.Date msgDate;
            if (dbTimestampStr == null || dbTimestampStr.trim().isEmpty()) {
                msgDate = new java.util.Date(); // real-time, use now
            } else {
                // dbTimestampStr is formatted as "yyyy-MM-dd HH:mm:ss"
                java.text.SimpleDateFormat dbFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                msgDate = dbFormat.parse(dbTimestampStr);
            }

            // check if today
            java.util.Calendar today = java.util.Calendar.getInstance();
            java.util.Calendar msgCal = java.util.Calendar.getInstance();
            msgCal.setTime(msgDate);

            boolean isToday = today.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR)
                    && today.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR);

            boolean isThisYear = today.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR);

            if (isToday) {
                // Format: 12h: "10:45 AM"
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a");
                return timeFormat.format(msgDate);
            } else if (isThisYear) {
                // Format: "29/05 10:45 AM"
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM hh:mm a");
                return dateFormat.format(msgDate);
            } else {
                // Format: "29/05/2025 10:45 AM"
                java.text.SimpleDateFormat fullFormat = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a");
                return fullFormat.format(msgDate);
            }
        } catch (Exception e) {
            // fallback in case of parsing error
            return dbTimestampStr != null ? dbTimestampStr : "";
        }
    }
}
