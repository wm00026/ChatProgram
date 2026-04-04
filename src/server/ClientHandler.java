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
 *      or dispatch commands via CommandDispatcher.
 *   4. Clean up on disconnect (planned or abrupt).
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ConcurrentHashMap<String, User> connectedUsers;
    private final ConcurrentHashMap<String, ChatRoom> rooms;
    private final ChatServer server;
    private final Logger logger;

    private User user;
    private String username;

    private PrintWriter out;
    private BufferedReader in;

    private ClientContext context;
    private CommandDispatcher dispatcher;

    private static final int MAX_MESSAGE_LENGTH = 500;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, User> connectedUsers,
            ConcurrentHashMap<String, ChatRoom> rooms, ChatRoom defaultRoom,
            ChatServer server, Logger logger) {
        this.socket = socket;
        this.connectedUsers = connectedUsers;
        this.rooms = rooms;
        this.server = server;
        this.logger = logger;
        // defaultRoom is passed to ClientContext after the handshake,
        // so we store it temporarily via a local approach in run()
        this.context = new ClientContext(null, null, defaultRoom, null);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if (!performHandshake()) {
                closeQuietly();
                return;
            }

            // Register the user
            user = new User(username, socket, out);
            connectedUsers.put(username, user);

            // Build the context and dispatcher now that we have a full user
            context = new ClientContext(username, out, context.getCurrentRoom(), user);
            context.getCurrentRoom().addMember(username);

            dispatcher = new CommandDispatcher(context, connectedUsers, rooms, server, logger);

            server.claimAdmin(username);
            if (server.isAdmin(username)) {
                out.println("[Server] You are the admin.");
            }

            out.println("Welcome to the chat, " + username + "! Type /help for commands.");
            context.getCurrentRoom().broadcastSystemMessage(
                    Protocol.joinMessage(username), username);

            // Message loop
            String rawInput;
            while ((rawInput = in.readLine()) != null) {
                rawInput = rawInput.trim();
                if (rawInput.isEmpty()) continue;

                if (rawInput.length() > MAX_MESSAGE_LENGTH) {
                    out.println("Message too long (max: " + MAX_MESSAGE_LENGTH + " chars)");
                    continue;
                }

                if (Protocol.isCommand(rawInput)) {
                    boolean shouldContinue = dispatcher.dispatch(rawInput);
                    if (!shouldContinue) break;
                } else {
                    handlePublicMessage(rawInput);
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost for: " + (username != null ? username : "unknown"));
        } finally {
            disconnect();
        }
    }

    /**
     * Prompts the client for a username, validates it, and checks uniqueness.
     * @return true if a valid username was accepted, false otherwise
     * @throws IOException if an I/O error occurs while reading from the client
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

            if (connectedUsers.containsKey(candidate.toLowerCase())) {
                out.println("Username '" + candidate + "' is already taken. Try again:");
                attempts++;
                continue;
            }

            if (server.isBanned(candidate.toLowerCase())) {
                out.println("Username '" + candidate + "' is banned from this server.");
                attempts++;
                continue;
            }

            username = candidate.toLowerCase();
            return true;
        }

        out.println("Failed to set a valid username. Disconnecting.");
        return false;
    }

    /**
     * Formats and broadcasts a public message to the current room.
     * @param text the message content
     */
    private void handlePublicMessage(String text) {
        if (server.isMuted(username)) {
            return;
        }

        Message msg = new Message(username, text, MessageType.PUBLIC);
        String formatted = msg.format();
        logger.log(context.getCurrentRoom().getName(), formatted);

        out.println(formatted); // Echo back to sender
        context.getCurrentRoom().broadcast(formatted, username);
    }

    /**
     * Cleans up on disconnect — removes user from registry and room,
     * notifies remaining users, and closes the socket.
     */
    private void disconnect() {
        if (username != null) {
            connectedUsers.remove(username);
            if (user != null) user.setActive(false);

            context.getCurrentRoom().removeMember(username);

            if (!connectedUsers.isEmpty()) {
                context.getCurrentRoom().broadcastSystemMessage(
                        Protocol.leaveMessage(username), username);
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