package server;

import common.Protocol;
import common.User;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CommandDispatcher
 *
 * Owns all command handling logic for a connected client.
 * Receives a command string via dispatch() and routes it to
 * the appropriate handler method.
 *
 * Separating this from ClientHandler keeps connection lifecycle
 * logic and command logic in distinct classes.
 */
public class CommandDispatcher {

    private final ClientContext context;
    private final ConcurrentHashMap<String, User> connectedUsers;
    private final ConcurrentHashMap<String, ChatRoom> rooms;
    private final ChatServer server;
    private final Logger logger;

    public CommandDispatcher(ClientContext context,
                             ConcurrentHashMap<String, User> connectedUsers,
                             ConcurrentHashMap<String, ChatRoom> rooms,
                             ChatServer server,
                             Logger logger) {
        this.context = context;
        this.connectedUsers = connectedUsers;
        this.rooms = rooms;
        this.server = server;
        this.logger = logger;
    }

    /**
     * Routes a command string to the appropriate handler.
     * @param input the raw command string from the client
     * @return false if the session should end (/quit), true otherwise
     */
    public boolean dispatch(String input) {
        if (input.equalsIgnoreCase(Protocol.CMD_LEAVE)) {
            return handleQuit();
        }

        if (input.equalsIgnoreCase(Protocol.CMD_LIST_USERS)) {
            handleList();
            return true;
        }

        if (input.equalsIgnoreCase(Protocol.CMD_HELP)) {
            handleHelp();
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_WHISPER)) {
            handleWhisper(input);
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_JOIN)) {
            handleJoin(input);
            return true;
        }

        if (input.equalsIgnoreCase(Protocol.CMD_ROOMS)) {
            handleRooms();
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_KICK)) {
            handleKick(input);
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_BAN)) {
            handleBan(input);
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_MUTE)) {
            handleMute(input);
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_UNMUTE)) {
            handleUnmute(input);
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_UNBAN)) {
            handleUnban(input);
            return true;
        }

        if (input.toLowerCase().startsWith(Protocol.CMD_TRANSFER_ADMIN)) {
            handleTransferAdmin(input);
            return true;
        }

        context.getOut().println(Protocol.errorMessage());
        return true;
    }

    // -------------------------
    // Handlers
    // -------------------------

    private void handleList() {
        StringBuilder sb = new StringBuilder("Connected users (")
                .append(connectedUsers.size()).append("): ");
        sb.append(String.join(", ", connectedUsers.keySet()));
        context.getOut().println(sb.toString());
    }

    private boolean handleQuit() {
        context.getOut().println("Goodbye, " + context.getUsername() + "!");
        return false;
    }

    private void handleHelp() {
        context.getOut().println(Protocol.getHelpMessage());
    }

    private void handleWhisper(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        String[] parts = Protocol.parseWhisperCommand(input);
        if (parts == null) {
            out.println("Usage: /whisper <username> <message>");
            return;
        }

        String recipientName = parts[0];
        String whisperText = parts[1];

        if (recipientName.equals(username)) {
            out.println("You can't whisper to yourself!");
            return;
        }

        User recipient = connectedUsers.get(recipientName);
        if (recipient == null || !recipient.isActive()) {
            out.println("User '" + recipientName + "' is not online.");
            return;
        }

        out.println(Protocol.whisperMessage(username, recipientName, whisperText));
        recipient.getOut().println(Protocol.whisperReceivedMessage(username, whisperText));
    }

    private void handleJoin(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();
        ChatRoom currentRoom = context.getCurrentRoom();

        String[] roomNameParts = Protocol.parseJoinCommand(input);
        
        if (roomNameParts == null) {
            out.println("Usage: /join <roomname> [password]");
            return;
        }

        String roomName = roomNameParts[0];
        String password = roomNameParts[1]; // null if not provided

        if (roomName.equalsIgnoreCase(currentRoom.getName())) {
            out.println("You are already in room " + roomName + ".");
            return;
        }

        // Check password if the room already exists
        ChatRoom existingRoom = server.getRoom(roomName);
        if (existingRoom != null && !existingRoom.checkPassword(password)) {
            out.println("Incorrect password for room '" + roomName + "'.");
            return;
        }

        String oldRoomName = currentRoom.getName();
        currentRoom.broadcastSystemMessage(Protocol.roomLeaveMessage(username, oldRoomName), username);
        currentRoom.removeMember(username);

        ChatRoom newRoom = server.getOrCreateRoom(roomName, password);
        newRoom.addMember(username);
        context.setCurrentRoom(newRoom);

        out.println("You joined " + roomName + ".");
        context.getCurrentRoom().broadcastSystemMessage(
                Protocol.roomJoinMessage(username, roomName), username);
        logger.log(context.getCurrentRoom().getName(), username + " joined " + roomName);
    }

    private void handleRooms() {
        context.getOut().println(server.getRoomList());
    }

    private void handleKick(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that.");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_KICK);
        if (target == null) {
            out.println("Usage: /kick <username>");
            return;
        }

        if (target.equals(username)) {
            out.println("You can't kick yourself.");
            return;
        }

        User targetUser = connectedUsers.get(target);
        if (targetUser == null) {
            out.println("User '" + target + "' not found.");
            return;
        }

        targetUser.getOut().println(String.format(Protocol.MSG_KICKED_FORMAT, target, username));
        server.kickUser(target);
        out.println("Kicked " + target + ".");
        context.getCurrentRoom().broadcastSystemMessage(
                target + " was kicked by " + username + ".", username);
    }

    private void handleBan(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that.");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_BAN);
        if (target == null) {
            out.println("Usage: /ban <username>");
            return;
        }

        if (target.equals(username)) {
            out.println("You can't ban yourself.");
            return;
        }

        User targetUser = connectedUsers.get(target);
        if (targetUser == null) {
            out.println("User '" + target + "' not found.");
            return;
        }

        targetUser.getOut().println(String.format(Protocol.MSG_BANNED_FORMAT, target, username));
        server.banUser(target);
        out.println("Banned " + target + ".");
        context.getCurrentRoom().broadcastSystemMessage(
                target + " was banned by " + username + ".", username);
    }

    private void handleMute(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that.");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_MUTE);
        if (target == null) {
            out.println("Usage: /mute <username>");
            return;
        }

        if (target.equals(username)) {
            out.println("You can't mute yourself.");
            return;
        }

        if (!server.muteUser(target)) {
            out.println("User '" + target + "' not found.");
            return;
        }

        out.println(String.format(Protocol.MSG_MUTED_FORMAT, target));
        context.getCurrentRoom().broadcastSystemMessage(
                target + " has been muted.", username);
    }

    private void handleUnmute(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that.");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_UNMUTE);
        if (target == null) {
            out.println("Usage: /unmute <username>");
            return;
        }

        server.unmuteUser(target);
        out.println(String.format(Protocol.MSG_UNMUTED_FORMAT, target));
        context.getCurrentRoom().broadcastSystemMessage(
                target + " has been unmuted.", username);
    }

    private void handleUnban(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that.");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_UNBAN);

        if (target == null) {
            out.println("Usage: /unban <username>");
            return;
        }

        if (!server.unbanUser(target)) {
            out.println("User '" + target + "' not found in ban list.");
            return;
        }

        out.println(String.format(Protocol.MSG_UNBANNED_FORMAT, target));
    }

    public void handleTransferAdmin(String input) {
        PrintWriter out = context.getOut();
        String username = context.getUsername();

        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that.");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_TRANSFER_ADMIN);
        if (target == null) {
            out.println("Usage: /transferadmin <username>");
            return;
        }


        if (target.equals(username)) {
            out.println("You are already the admin.");
            return;
        }

        User targetUser = connectedUsers.get(target);
        if (targetUser == null) {
            out.println("User '" + target + "' not found.");
            return;
        }

        if (server.isBanned(target)) {
            out.println("You can't transfer admin to a banned user.");
            return;
        }

        server.transferAdmin(target);
        out.println(String.format(Protocol.MSG_TRANSFER_ADMIN_FORMAT, target));
        targetUser.getOut().println("You are now the admin");
    }
}