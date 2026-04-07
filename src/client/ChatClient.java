package client;

import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;

    private void initGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(600, 450);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        inputField.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (input.isEmpty()) return;
            
            out.println(input);
            inputField.setText("");

            if (input.equalsIgnoreCase(Protocol.CMD_LEAVE)) {
                shutdown();
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (running && out != null) {
                    out.println(Protocol.CMD_LEAVE);
                }
                shutdown();
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputField, BorderLayout.SOUTH);
        frame.setVisible(true);
        inputField.requestFocusInWindow();
    }

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
        appendMessage("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
    }

    /**
     * Starts the listener thread, then runs the sender loop on the
     * current (main) thread. Returns when the session ends.
     */
    public void start() {
        Thread listenerThread = new Thread(this::listenForMessages, "listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Listens for messages from server and prints to console.
     * Handles disconnect from server IF server is shutdown.
     */
    private void listenForMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                appendMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                appendMessage("Lost connection to server: " + e.getMessage());
            }
        } finally {
            // If the server dropped us, shut down the sender side too
            shutdown();
        }
    }


    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + " " + message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }


    /**
     * Handles shutting down client.
     */
    private void shutdown() {
        if (!running) return; // already shutting down
        running = false;
        appendMessage("Disconnected");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
        }
    }

    // Main method to start client.
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        ChatClient client = new ChatClient();
        try {
            client.initGUI();
            client.connect();
            client.start();
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null, "Server not found: " + SERVER_HOST);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                "Could not connect to server: " + e.getMessage() +
                "\nIs ChatServer running on port " + SERVER_PORT + "?");
        }
    });
}

}