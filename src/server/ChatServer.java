package server;

import common.Message;
import common.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main chat server:
 * Binds to a port, accepts incoming clinet connections in a loop,
 * and spawns a ClientHandler for each one.
 * All connected users are tracked in a shared ConcurrentHashMap.
 */
public class ChatServer {


    private static final int PORT = 12345;

    // Shared user registry: username -> User object

    private final ConcurrentHashMap<String, User> connectedUsers = new ConcurrentHashMap<>();

    /**
     * Starts the chat server: opens a ServerSocket and listens for incoming connections.
     */

    public void start() {
        System.out.println("Chat server starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started. Waiting for clients.");
            
            while (true) {
                // accept() blocks until a client connects
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());

                // Each client gets its own handler
                ClientHandler handler = new ClientHandler(clientSocket, connectedUsers);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all connected users.
     * Called by ClientHandler instance on the shared server context.
     * @param message The message to broadcast.
     * @param senderName the username to skip (so sender doesn't echo)
     */

    public void broadcast(Message message, String senderName) {
        String formatted = message.format();
        connectedUsers.forEach((username, user) -> {
            if (!username.equals(senderName) && user.isActive()) {
                user.getOut().println(formatted);
            }
         });
    }

    /**
     * Sends a message to a specific user
     * @param message
     * @param recipient
     * @return
     */
    public boolean sendToUser(Message message, String recipient) {
        User target = connectedUsers.get(recipient);
        if (target == null || !target.isActive()) {
            return false;
        }

        target.getOut().println(message.format());
        return true;
    }

    public String getUserList() {
        if (connectedUsers.isEmpty()) {
            return "No users connected.";
        }

        StringBuilder sb = new StringBuilder("Connected users: ");
        sb.append(String.join(", ", connectedUsers.keySet()));
        return sb.toString();
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
    
}
