package server;

import common.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;

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
        rooms.put(DEFAULT_ROOM, new ChatRoom(DEFAULT_ROOM, connectedUsers, null));
    }

    public ChatServer(int port) {
        this.PORT = port;
        rooms.put(DEFAULT_ROOM, new ChatRoom(DEFAULT_ROOM, connectedUsers, null));
    }

    // Shared user registry: username -> User object

    private final ConcurrentHashMap<String, User> connectedUsers = new ConcurrentHashMap<>();

    private final Logger logger = new Logger();

    private String adminUsername = null;
    private final Set<String> mutedUsers = ConcurrentHashMap.newKeySet();
    private final Set<String> bannedUsers = ConcurrentHashMap.newKeySet();

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
                ClientHandler handler = new ClientHandler(clientSocket, connectedUsers, rooms, defaultRoom, this, logger);
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
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            logger.close();
            System.out.println("Server shutdown complete.");
        }
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
   
    public ChatRoom getOrCreateRoom(String roomName, String password) {
        return rooms.computeIfAbsent(roomName, name -> new ChatRoom(name, connectedUsers, password));
    }

    /**
     * Gets a list of all existing chat rooms
     * @return the formatted room list string
     */
    public String getRoomList() {
        StringBuilder sb = new StringBuilder("Rooms (").append(rooms.size()).append("): ");
        rooms.forEach((name, room) ->
            sb.append(name).append("(").append(room.getMemberCount()).append(") "));
        return sb.toString().trim();
    }

    /**
     * Claims admin privileges for a user if no admin exists yet.
     * @param username
     */
    public synchronized void claimAdmin(String username) {
        if (adminUsername == null) {
            adminUsername = username;
            System.out.println("Admin claimed by: " + username);
        }
    }

    public synchronized void transferAdmin(String newAdmin) {
            adminUsername = newAdmin;
            System.out.print("Admin transferred to: " + newAdmin);
    }

    // Verifys if a given username is the current admin
    public boolean isAdmin(String username) {
        return username != null && username.equals(adminUsername);
    }

    // Verifies if a given username is banned
    public boolean isBanned(String username) {
        return bannedUsers.contains(username);
    }

    // Mutes a user; adding them to the mutedUsers set
    public boolean muteUser(String target) {
        if (connectedUsers.containsKey(target)) {
            mutedUsers.add(target);
            return true;
        }
        return false;
    }

    // Unmutes a user; removing them from the mutedUsers set
    public void unmuteUser(String target) {
        mutedUsers.remove(target);
    }

    // Verifys if a user is muted.
    public boolean isMuted(String username) {
        return mutedUsers.contains(username);
    }


    public boolean kickUser(String target) {
        User user = connectedUsers.get(target);
        if (user == null) {
            return false;
        }
        try {
            user.getSocket().close();
        } catch (IOException e) {
            System.err.println("Error kicking " + target + ": " + e.getMessage());
        }
        return true;
    }

    public boolean banUser(String target) {
            bannedUsers.add(target);
            return kickUser(target);
    }

    public boolean unbanUser(String target) {
        return bannedUsers.remove(target);
    }

    // Runs the chat server
    public static void main(String[] args) {
        new ChatServer().start();
    }
    
}
