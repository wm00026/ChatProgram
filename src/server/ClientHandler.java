package server;

import common.Message;
import common.Message.MessageType;
import common.User;
import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClientHandler
 *
 * A Runnable executed on its own thread for each connected client.
 * Responsibilities:
 *   1. Negotiate a valid, unique username with the new client.
 *   2. Register the User in the shared connectedUsers map.
 *   3. Read incoming messages in a loop and either broadcast them
 *      or dispatch commands (/list, /quit, /whisper, /help).
 *   4. Clean up on disconnect (planned or abrupt).
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ConcurrentHashMap<String, User> connectedUsers;
    
    private User user;
    private String username;

    private PrintWriter out;
    private BufferedReader in;

    private final ConcurrentHashMap<String, ChatRoom> rooms;
    private ChatRoom currentRoom;
    private final Logger logger;
    private final ChatServer server;

    private static final int MAX_MESSAGE_LENGTH = 500; 

    public ClientHandler(Socket socket, ConcurrentHashMap<String, User> connectedUsers, 
        ConcurrentHashMap<String, ChatRoom> rooms, ChatRoom defaultRoom, ChatServer server, Logger logger) {
        this.socket = socket;
        this.connectedUsers = connectedUsers;
        this.rooms = rooms;
        this.currentRoom = defaultRoom;
        this.server= server;
        this.logger = logger;
    }

    // Main loop for handling client communication
    @Override
    public void run() {
        try {

            // Wrap the raw streams once so every method can use them.
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Get valid username 
            if (!performHandshake()) {
                closeQuietly();
                return;
            }

            // Register the user
            user = new User(username, socket, out);
            connectedUsers.put(username, user);
            currentRoom.addMember(username);

            server.claimAdmin(username);
            if (server.isAdmin(username)) {
                out.println("[Server] You are the admin");
            }

            out.println("Welcome to the chat, " + username +"! Type /help for commands.");
            broadcastSystemMessage(Protocol.joinMessage(username), username);

            // Message loop: read until client disconnects or quits.
            String rawInput;
            while ((rawInput = in.readLine()) != null) {
                rawInput = rawInput.trim();
                if (rawInput.isEmpty()) continue;

                if (rawInput.length() > MAX_MESSAGE_LENGTH) {
                    out.println("Message too long (max: " + MAX_MESSAGE_LENGTH + "chars)");
                }

                if (Protocol.isCommand(rawInput)) {
                    boolean shouldContinue = handleCommand(rawInput);
                    if (!shouldContinue) break;
                } else {
                    handlePublicMessage(rawInput);
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost for: " + (username != null ? username : "unknown"));
        } finally {
            // Always clean up
            disconnect();
        }
    }

    /**
     * Handshake: Prompts client for username, valdiates, and checks uniqueness.
     * returns true once a valid username is accepted
     * false if client disconnected or fails too many times
     * @throws IOException if an I/O error occurs while reading from client
     */

    private boolean performHandshake() throws IOException {
        out.println("Enter a username (3-" + Protocol.MAX_USERNAME_LENGTH + " chars, letters/numbers/underscores):");

        String candidate;
        int attempts = 0;
        final int MAX_ATTEMPTS = 5;

        while ((candidate = in.readLine()) != null && attempts < MAX_ATTEMPTS) {
            candidate = candidate.trim();

            if (!Protocol.isValidUsername(candidate)) {
                out.println("Invalid username. Try again:");
                attempts++;
                continue;
            }

            if (connectedUsers.containsKey(candidate)) {
                out.println("Username '" + candidate + "' is already taken. Try again:");
                attempts++;
                continue;
            }

            if (server.isBanned(candidate)) {
                out.println("Username '" + candidate + " ' is banned from server");
                attempts++;
                continue;
            }

            username = candidate;
            return true;
        }

        out.println("Failed to set a valid username. Disconnecting");
        return false;
    }

    /**
     * Handles a public message: formats, broadcasts to others, and echoes back to sender.
     * @param text the content of the message
     */
    private void handlePublicMessage(String text) {
        if (server.isMuted(username)) {
            return;
        }

        Message msg = new Message(username, text, MessageType.PUBLIC);
        String formatted = msg.format();
        logger.log(currentRoom.getName(), formatted);

        out.println(formatted); // Echo back
        currentRoom.broadcast(formatted, username);
    }

        /**
         * Handles a command input. Directs to appropriate handler, or returns an unknown command.
         * @param input the command input from client
         * @return true; either using the command or priting an error message and continuing the session.
         */
        private boolean handleCommand(String input) {
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

        if(input.toLowerCase().startsWith(Protocol.CMD_JOIN)) {
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
 
        // Unknown command — tell the user but keep the session alive.
        out.println(Protocol.errorMessage());
        return true;
    }

    
    /**
     * Handlers: Each command has a handler to keep code organized and manage command formatting.
     */

    private void handleList() {
        StringBuilder sb = new StringBuilder("Connected users (").append(connectedUsers.size()).append("): ");
        sb.append(String.join(", ", connectedUsers.keySet()));
        out.println(sb.toString());
    }

    private boolean handleQuit() {
        out.println("Goodbye, " + username + "!");
        return false; 
    }

    private void handleHelp() {
        out.println(Protocol.getHelpMessage());
    }

    private void handleWhisper(String input) {
        String[] parts = Protocol.parseWhisperCommand(input);

        if (parts == null) {
            out.println("Usage: /whisper <username> <message>");
            return;
        }

        String recipientName = parts[0];
        String whisperText = parts[1];

        if (recipientName.equalsIgnoreCase(username)) { 
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
        String roomName = Protocol.parseJoinCommand(input);

        if (roomName == null) {
            out.println("Usage /join <roomname>");
            return;
        }

        if (roomName.equalsIgnoreCase(currentRoom.getName())) {
            out.println("You are already in room " + roomName + ".");
            return;
        }

        String oldRoomName = currentRoom.getName();
        currentRoom.broadcastSystemMessage(Protocol.roomLeaveMessage(username, oldRoomName), username);
        currentRoom.removeMember(username);

        ChatRoom newRoom = server.getOrCreateRoom(roomName);
        newRoom.addMember(username);
        currentRoom = newRoom;
        
        out.println("You joined " + roomName + ".");
        currentRoom.broadcastSystemMessage(Protocol.roomJoinMessage(username, roomName), username);
        logger.log(currentRoom.getName(), username + " joined " + roomName);

    }

    private void handleRooms() {
        out.println(server.getRoomList());
    }

    private void handleKick(String input) {
        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_KICK);
        if (target == null) {
            out.println("Usage: /kick <username>");
            return;
        }

        if (target.equalsIgnoreCase(username)) {
            out.println("You can't kick yourself");
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
        currentRoom.broadcastSystemMessage(target + " was kicked by " + username + ".", username);
    }

    private void handleBan(String input) {
        if (!server.isAdmin(username)) {
            out.println("You don't have permission to do that");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_BAN);
        if (target == null) {
            out.println("Usage: /ban <username>");
            return;
        }

        if (target.equalsIgnoreCase(username)) {
            out.println("You can't ban yourself");
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
        currentRoom.broadcastSystemMessage(target + " was banned by " + username + ".", username);
    }

    private void handleMute(String input) {
        if (!server.isAdmin(username)) {
            out.println("You don't have permission to handle that");
            return;
        }

        String target = Protocol.parseTargetCommand(input, Protocol.CMD_MUTE);
        if (target == null) {
            out.println("Usage: /mute <username>");
            return;
        }

        if (target.equalsIgnoreCase(username)) {
            out.println("You can't mute yourself");
            return;
        }

        if (!server.muteUser(target)) {
            out.println("User '" + target + "' not found.");
            return;
        }

        out.println(String.format(Protocol.MSG_MUTED_FORMAT, target));
        currentRoom.broadcastSystemMessage(target + " has been muted.", username);
    }

    private void handleUnmute(String input) {
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
    currentRoom.broadcastSystemMessage(target + " has been unmuted.", username);
    } 
 
    /**
     * Broadcasts a system message to all users, except the excluded username.
     * @param content the content of the message.
     * @param excludeUserName the username to exclude fromr receiving the message
     */
    private void broadcastSystemMessage(String content, String excludeUserName) {
        currentRoom.broadcastSystemMessage(content, excludeUserName);
    }

        /**
         * Handler disconnecting a user
         */
        private void disconnect() {
        if (username != null) {
            connectedUsers.remove(username);
            if (user != null) user.setActive(false);
 
            currentRoom.removeMember(username);

            // Only broadcast if other users are still connected.
            if (!connectedUsers.isEmpty()) {
                broadcastSystemMessage(Protocol.leaveMessage(username), username);
            }
 
            System.out.println(username + " has disconnected.");
        }
        closeQuietly();

    }
 
    /** Closes the socket, swallowing any IOException. */
    private void closeQuietly() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Nothing useful to do here.
        }
    }
}
