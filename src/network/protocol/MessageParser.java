package network.protocol;

public class MessageParser {

    public static String encode(Message msg) {
        if (msg == null)
            return null;
        return String.format("%s|%s|%s|%s", msg.getType(), msg.getSender(), msg.getTarget(), msg.getContent());
    }

    public static Message decode(String raw) {
        if (raw == null || raw.trim().isEmpty())
            return null;

        // 4 -> split the string into at most 4 parts
        String[] parts = raw.split("\\|", 4);

        if (parts.length < 4) {
            System.out.println("[PARSER ERROR]: String is not enough 4 fields " + raw);
            return null;
        }

        try {

            MessageType type = MessageType.valueOf(parts[0]); // prevent unknown type
            String sender = parts[1];
            String target = parts[2];
            String content = parts[3];

            return new Message(type, sender, target, content);
        } catch (IllegalArgumentException e) {
            System.out.println("[PARSER ERROR]: The message type is INVALID!");
            return null;
        }
    }

}
