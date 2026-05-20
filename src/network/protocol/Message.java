package network.protocol;

import java.io.Serializable;

// protocol format: TYPE|SENDER|TARGET|CONTENT
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String target;
    private String content;

    private String fileId;
    private String filename;
    private byte[] fileData; // for file transfer

    public Message(MessageType type, String sender, String target, String content) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.content = (content == null) ? "" : content;
    }

    public Message(MessageType type, String sender, String target, String filename, String fileId, byte[] fileData) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.filename = filename;
        this.fileId = fileId;
        this.fileData = fileData;
        this.content = ""; // unused for files
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

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s", type, sender, target, content);
    }

}
