package server;

import common.Message;
import common.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The main chat server:
 * Binds to a port, accepts incoming clinet connections in a loop,
 * and spawns a ClientHandler for each one.
 * All connected users are tracked in a shared ConcurrentHashMap.
 */
public class ChatServer {


    private final int PORT;

    private final ConcurrentHashMap<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private static final String DEFAULT_ROOM = "lobby";

    public ChatServer() {
        this.PORT = 12345;
        rooms.put(DEFAULT_ROOM, new ChatRoom(DEFAULT_ROOM, connectedUsers));
    }

    public ChatServer(int port) {
        this.PORT = port;
        rooms.put(DEFAULT_ROOM, new ChatRoom(DEFAULT_ROOM, connectedUsers));
    }

    // Shared user registry: username -> User object

    private final ConcurrentHashMap<String, User> connectedUsers = new ConcurrentHashMap<>();

    private static final int MAX_THREADS = 50;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);



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

                ChatRoom defaultRoom = rooms.get(DEFAULT_ROOM);
                ClientHandler handler = new ClientHandler(clientSocket, connectedUsers, rooms, defaultRoom, this);
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("Server stopped: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Shuts down the server and thread pool gracefully
     */
    private void shutdown() {
        System.out.println("Shutting down thread pool...");
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                System.out.println("Thread pool forced to shutdown.");
            }
            else {
                System.out.println("Thread pool shutdown complete.");
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
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
     * @param message the messege to sender
     * @param recipient the person receiving the message
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

    // Gets the user list, separated by commas.
    public String getUserList() {
        if (connectedUsers.isEmpty()) {
            return "No users connected.";
        }

        StringBuilder sb = new StringBuilder("Connected users: ");
        sb.append(String.join(", ", connectedUsers.keySet()));
        return sb.toString();
    }

    public ChatRoom getRoom(String roomName) {
        return rooms.get(roomName);
    }

    /**
     * Either gets an existing chat room by name OR
     * creates a new one if it doesn't exist
     * @param roomName the name of the room
     * @return the existing or new ChatRoom instance
     */
   
    public ChatRoom getOrCreateRoom(String roomName) {
        return rooms.computeIfAbsent(roomName, name -> new ChatRoom(name, connectedUsers));
    }

    /**
     * Gets a list of all existing chat rooms
     * @return
     */
    public String getRoomList() {
        StringBuilder sb = new StringBuilder("Rooms (").append(rooms.size()).append("): ");
        rooms.forEach((name, room) ->
            sb.append(name).append("(").append(room.getMemberCount()).append(") "));
        return sb.toString().trim();
    }

    // Runs the chat server
    public static void main(String[] args) {
        new ChatServer().start();
    }
    
}
