package client;

import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * ChatClient
 *
 * Connects to the ChatServer and runs two threads:
 * A listener thread that listens for messages from server and prints to console
 * A sender thread that handles I/O from user.
*/
public class ChatClient {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Signals both threads to stop when the session ends.
    private volatile boolean running = false;

    /**
     * Opens the socket connection and wires up the streams.
     * Throws if the server isn't reachable.
     * @throws IOException if an I/O error occurs when creating the socket or streams
     */
    public void connect() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        // autoFlush=true so every println() sends immediately

        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        System.out.println("Connected to " + SERVER_HOST + ":" + SERVER_PORT);
    }

    /**
     * Starts the listener thread, then runs the sender loop on the
     * current (main) thread. Returns when the session ends.
     */
    public void start() {
        Thread listenerThread = new Thread(this::listenForMessages, "listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        sendMessages();
    }

    /**
     * Listens for messages from server and prints to console.
     * Handles disconnect from server IF server is shutdown.
     */
    private void listenForMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Lost connection to server: " + e.getMessage());
            }
        } finally {
            // If the server dropped us, shut down the sender side too
            shutdown();
        }
    }

    /**
     * Handles user messages to be sent to server.
     */
    private void sendMessages() {
        try (BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while (running && (input = keyboard.readLine()) != null) {
                input = input.trim();
                if (input.isEmpty()) continue;

                out.println(input);


                if (input.equalsIgnoreCase(Protocol.CMD_LEAVE)) {
                    break;
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Input error: " + e.getMessage());
            }
        } finally {
            shutdown();
        }
    }


    /**
     * Handles shutting down client.
     */
    private void shutdown() {
        if (!running) return; // already shutting down
        running = false;
        System.out.println("Disconnected.");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
        }
    }

    // Main method to start client.
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        try {
            client.connect();
            client.start(); // blocks until the session ends
        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + SERVER_HOST);
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("Is ChatServer running on port " + SERVER_PORT + "?");
        }
    }
}