package common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Message class
 * Sets the standardization of messages within the chat program
 */
public class Message {
    private String sender;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String recipient;

    public enum MessageType {
        PUBLIC, PRIVATE, SYSTEM, JOIN, LEAVE, ERROR
    }

    /**
     * Constructor for message class w/o recipient
     * @param sender
     * @param content
     * @param type
     */
    public Message(String sender, String content, MessageType type) {
        this(sender, content, type, null);
    }


    /**
     * General constructor for the message class
     * @param sender the user that is sending the message
     * @param content the message itself
     * @param type the type of message sent
     * @param recipient the user that is getting the message
     */
    public Message(String sender, String content, MessageType type, String recipient) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.recipient = recipient;
        this.timestamp = LocalDateTime.now();
    }

    public String format() {
        String time = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        switch(type) {
            case PRIVATE:
                return String.format("[%s] [PM from %s] %s", time, sender, content);
            case SYSTEM:
                return String.format("[%s] [SYSTEM] %s", time, content);
            case JOIN:
            case LEAVE:
                return String.format("[%s] %s", time, content);
            default:
                return String.format("[%s] %s: %s", time, sender, content);
        }
    }

    // Getters
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public MessageType getType() { return type; }
    public String getRecipient() { return recipient; }
}
