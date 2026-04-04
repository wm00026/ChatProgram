package common;

/**
 * Common message format and protocols
 */
public class Protocol {

    // Standard Message Structure
    public static final String STANDARD_MSG_FORMAT = "[%s] %s";
    public static final String MSG_JOIN_FORMAT = "[%s has joined!]";
    public static final String MSG_LEAVE_FORMAT = "[%s has left]";
    public static final String MSG_ERROR_FORMAT = "Command not recognized. Type /help for a list of commands.";
    public static final String MSG_WHISPER_FORMAT = "[%s (whisper to %s)] %s";
    public static final String MSG_WHISPER_RECEIVED_FORMAT = "[%s whispers] %s";
    public static final String MSG_ROOM_JOIN_FORMAT = "[%s has joined %s]";
    public static final String MSG_ROOM_LEAVE_FORMAT = "[%s has left %s]";

    // Admin Message Structure:
    public static final String MSG_KICKED_FORMAT = "[%s has been kicked by %s]";
    public static final String MSG_BANNED_FORMAT = "[%s has been banned by %s]";
    public static final String MSG_MUTED_FORMAT = "[%s has been muted]";
    public static final String MSG_UNMUTED_FORMAT = "[%s has been unmuted]";

    // Commands
    public static final String CMD_LIST_USERS = "/list";
    public static final String CMD_LEAVE = "/quit";
    public static final String CMD_WHISPER = "/whisper"; // Stretch goal
    public static final String CMD_HELP = "/help";
    public static final String CMD_JOIN = "/join";
    public static final String CMD_ROOMS = "/rooms";

    // Admin commands
    public static final String CMD_KICK = "/kick";
    public static final String CMD_BAN = "/ban";
    public static final String CMD_MUTE = "/mute";
    public static final String CMD_UNMUTE = "/unmute";

    // Username validation constants
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final String VALID_USERNAME_PATTERN = "^[a-zA-Z0-9_]+$";

    /**
     * Formats a standard chat message
     * @param username The sender's username
     * @param message The message content
     * @return Formatted message string
     */
    public static String standardMessage(String username, String message) {
        return String.format(STANDARD_MSG_FORMAT, username, message);
    }

    /**
     * Formats a user join notification
     * @param username The user who joined
     * @return Formatted join message
     */
    public static String joinMessage(String username) {
        return String.format(MSG_JOIN_FORMAT, username);
    }

    /**
     * Formats a user leave notification
     * @param username The user who left
     * @return Formatted leave message
     */
    public static String leaveMessage(String username) {
        return String.format(MSG_LEAVE_FORMAT, username);
    }

    /**
     * Formats an error message
     * @return Error message string
     */
    public static String errorMessage() {
        return MSG_ERROR_FORMAT;
    }

    /**
     * Formats a whisper message for the sender
     * @param sender The sender's username
     * @param recipient The recipient's username
     * @param message The whisper content
     * @return Formatted whisper message for sender
     */
    public static String whisperMessage(String sender, String recipient, String message) {
        return String.format(MSG_WHISPER_FORMAT, sender, recipient, message);
    }

    /**
     * Formats a whisper message for the recipient
     * @param sender The sender's username
     * @param message The whisper content
     * @return Formatted whisper message for recipient
     */
    public static String whisperReceivedMessage(String sender, String message) {
        return String.format(MSG_WHISPER_RECEIVED_FORMAT, sender, message);
    }

    /**
     * Validates a username
     * @param username The username to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        username = username.trim();

        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return false;
        }

        // Check for valid characters (alphanumeric and underscore)
        return username.matches(VALID_USERNAME_PATTERN);
    }

    /**
     * Checks if a message is a command
     * @param message The message to check
     * @return true if it's a command, false otherwise
     */
    public static boolean isCommand(String message) {
        return message != null && message.startsWith("/");
    }

    /**
     * Parses a whisper command
     * @param message The full message
     * @return String array with [recipient, message] or null if invalid
     */
    public static String[] parseWhisperCommand(String message) {
        if (message == null || !message.startsWith(CMD_WHISPER)) {
            return null;
        }

        // Remove the command prefix and trim
        String content = message.substring(CMD_WHISPER.length()).trim();

        // Find the first space to separate recipient and message
        int firstSpace = content.indexOf(' ');
        if (firstSpace == -1) {
            return null; // No message content
        }

        String recipient = content.substring(0, firstSpace).trim().toLowerCase();
        String whisperMessage = content.substring(firstSpace + 1).trim();

        if (recipient.isEmpty() || whisperMessage.isEmpty()) {
            return null;
        }

        return new String[]{recipient, whisperMessage};
    }

    /**
     * Formats a room join message
     * @param username the username of the user joining the room
     * @param roomName the name of the room the user is joining
     * @return the formatted room join message
     */
    public static String roomJoinMessage(String username, String roomName) {
        return String.format(MSG_ROOM_JOIN_FORMAT, username, roomName);
    }

    /**
     * Formats a room leave message
     * @param username the username of the user leaving the room
     * @param roomName the name of the room the user is leaving
     * @return the formatted room leave message
     */
    public static String roomLeaveMessage(String username, String roomName) {
        return String.format(MSG_ROOM_LEAVE_FORMAT, username, roomName);
    }

    /**
     * parses a join command to extract the room name.
     * @param input the full command input from the user
     * @return the room name if the command is valid, or null if its invalid or missing roomName.
     */
    public static String parseJoinCommand(String input) {
        if (input == null || !input.toLowerCase().startsWith(CMD_JOIN)) {
            return null;
        }

        String roomName = input.substring(CMD_JOIN.length()).trim();
        return roomName.isEmpty() ? null : roomName;
    }

    public static String parseTargetCommand(String input, String cmd) {
        if (input == null || !input.toLowerCase().startsWith(cmd)) {
            return null;
        }

        String target = input.substring(cmd.length()).trim().toLowerCase();
        return target.isEmpty() ? null : target;
    }

    /**
     * Gets the help message with available commands
     * @return Help message string
     */
    public static String getHelpMessage() {
        StringBuilder help = new StringBuilder();
        help.append("Available Commands:\n");
        help.append("  ").append(CMD_LIST_USERS).append(" - Show all users in chat\n");
        help.append("  ").append(CMD_LEAVE).append(" - Exit the chat\n");
        help.append("  ").append(CMD_WHISPER).append(" <username> <message> - Send private message\n");
        help.append("  ").append(CMD_HELP).append(" - Show this help message\n");
        help.append("  ").append(CMD_JOIN).append(" <room> - Join or create a room\n");
        help.append("  ").append(CMD_ROOMS).append(" - List all rooms\n");
        help.append("  ").append(CMD_KICK).append(" <user> - Kick a user (admin only)\n");
        help.append("  ").append(CMD_BAN).append(" <user> - Ban a user (admin only)\n");
        help.append("  ").append(CMD_MUTE).append(" <user> - Mute a user (admin only)\n");
        help.append("  ").append(CMD_UNMUTE).append(" <user> - Unmute a user (admin only)\n");
        return help.toString();
    }

    // Prevents instantiation
    private Protocol() {}

}