package common.protocol;

import java.io.Serializable;
import java.util.List;

// protocol format: TYPE|SENDER|TARGET|CONTENT
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private int messageId;
    private List<Message> historyList;

    private MessageType type;
    private String sender;
    private String target;
    private String content;
    private String displayName;

    private String fileId;
    private String filename;
    private byte[] fileData; // for file transfer

    public Message(MessageType type, String sender, String target, String content) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.content = (content == null) ? "" : content;
    }

    // constructor for history response
    public Message(MessageType type, String sender, String target, List<Message> historyList) {
    this.type = type;
    this.sender = sender;
    this.target = target;
    this.historyList = historyList;
}

    // constructor for file transfer
    public Message(MessageType type, String sender, String target, String filename, String fileId, byte[] fileData) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.filename = filename;
        this.fileId = fileId;
        this.fileData = fileData;
        this.content = ""; // unused for files
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public List<Message> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<Message> historyList) {
        this.historyList = historyList;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getTarget() {
        return target;
    }

    public String getContent() {
        return content;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFilename() {
        return filename;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s", type, sender, target, content);
    }

}
