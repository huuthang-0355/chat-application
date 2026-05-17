package network.protocol;

import java.io.Serializable;

// protocol format: TYPE|SENDER|TARGET|CONTENT
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String target;
    private String content;

    public Message(MessageType type, String sender, String target, String content) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.content = (content == null) ? "" : content;
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

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s", type, sender, target, content);
    }

}
