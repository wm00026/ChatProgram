package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatServerIntegrationTest
 *
 * Spins up a real ChatServer on a background thread and connects
 * real client sockets to it. Tests the full path from raw TCP input
 * to formatted output — covering handshake, messaging, and disconnect.
 *
 * Structure:
 *   - TestClient      inner class: wraps a Socket with a reader/writer for convenience
 *   - @BeforeEach     starts the server in a daemon thread before every test
 *   - @AfterEach      closes all open clients after every test
 *   - Tests           one scenario per test method
 */
public class ChatServerIntegrationTest {

    // Port to run the test server on — separate from the production default
    private static final int TEST_PORT = 12346;

    // How long (ms) to wait for a server response before failing
    private static final int TIMEOUT_MS = 3000;

    // Tracks clients opened during a test so @AfterEach can close them
    private TestClient clientA;
    private TestClient clientB;

    // =========================================================================
    // Setup and teardown
    // =========================================================================

    /**
     * Starts a fresh ChatServer on a background daemon thread before each test.
     * The 100 ms sleep gives the ServerSocket time to bind before clients connect.
     */
    @BeforeEach
    void startServer() throws InterruptedException {
        Thread serverThread = new Thread(() -> new ChatServer(TEST_PORT).start());
        serverThread.setDaemon(true); // dies automatically when the test JVM exits
        serverThread.start();
        waitForServerReady();
    }

    private void waitForServerReady() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000; 
        while (System.currentTimeMillis() < deadline) {
            try (Socket probe = new Socket("localhost", TEST_PORT)) {
                BufferedReader probeIn = new BufferedReader(new InputStreamReader(probe.getInputStream()));
                PrintWriter probeOut = new PrintWriter(probe.getOutputStream(), true);

                probeIn.readLine();
                probeOut.println("__probe__");
                probeIn.readLine();
                return;
            } catch (IOException e) {
                Thread.sleep(20);
            }
        }
        throw new IllegalStateException("Server did not start within timeout");
    }

    /**
     * Closes any clients opened during the test.
     * Runs even if the test threw an exception, keeping ports clean.
     */
    @AfterEach
    void closeClients() {
        if (clientA != null) clientA.close();
        if (clientB != null) clientB.close();
    }

    // =========================================================================
    // Tests
    // =========================================================================

    @Test
    @DisplayName("valid username is accepted and welcome message is received")
    void handshake_validUsername_receivesWelcome() throws IOException {
        clientA = new TestClient(TEST_PORT);

        // Server always sends the prompt first
        String prompt = clientA.readLine();
        assertTrue(prompt.contains("username"), "server should prompt for a username");

        // Send a valid username
        clientA.sendLine("Alice");

        // Drain lines until we find the welcome message
        String response = clientA.readUntilContains("Welcome");
        assertTrue(response.contains("Welcome"), "server should send a welcome message");
        assertTrue(response.contains("Alice"), "welcome message should include the username");
    }

    @Test
    @DisplayName("invalid username is rejected, valid retry is then accepted")
    void handshake_invalidThenValid_recovers() throws IOException {
        clientA = new TestClient(TEST_PORT);

        clientA.readLine(); // consume the prompt

        // Send a username that fails validation (too short)
        clientA.sendLine("ab");
        String rejection = clientA.readLine();
        assertTrue(rejection.toLowerCase().contains("invalid"),
                "server should reject an invalid username");

        // Now send a valid one — session should proceed
        clientA.sendLine("Alice");
        String welcome = clientA.readUntilContains("Welcome");
        assertTrue(welcome.contains("Welcome"), "server should accept the valid retry");
    }

    @Test
    @DisplayName("duplicate username is rejected, second client must choose another name")
    void handshake_duplicateUsername_rejected() throws IOException {
        // First client claims "Alice"
        clientA = new TestClient(TEST_PORT);
        clientA.readLine(); // prompt
        clientA.sendLine("Alice");
        clientA.readUntilContains("Welcome"); // wait until fully connected

        // Second client tries the same name
        clientB = new TestClient(TEST_PORT);
        clientB.readLine(); // prompt
        clientB.sendLine("Alice");

        String response = clientB.readLine();
        assertTrue(response.toLowerCase().contains("taken"),
                "server should report the username is already taken");
    }

    @Test
    @DisplayName("message sent by one client is received by another")
    void messaging_publicMessage_isBroadcast() throws IOException {
        // Connect both clients
        clientA = new TestClient(TEST_PORT);
        clientA.readLine();
        clientA.sendLine("Alice");
        clientA.readUntilContains("Welcome");

        clientB = new TestClient(TEST_PORT);
        clientB.readLine();
        clientB.sendLine("Bob");
        clientB.readUntilContains("Welcome");

        // Alice sends a message
        clientA.sendLine("Hello from Alice!");

        // Bob should receive it
        String received = clientB.readUntilContains("Hello from Alice!");
        assertTrue(received.contains("Hello from Alice!"),
                "Bob should receive Alice's message");
        assertTrue(received.contains("Alice"),
                "received message should include the sender's name");
    }

    @Test
    @DisplayName("/quit sends a goodbye message and ends the session")
    void disconnect_quit_receivesGoodbye() throws IOException {
        clientA = new TestClient(TEST_PORT);
        clientA.readLine();
        clientA.sendLine("Alice");
        clientA.readUntilContains("Welcome");

        clientA.sendLine("/quit");

        String goodbye = clientA.readUntilContains("Goodbye");
        assertTrue(goodbye.contains("Goodbye"),
                "server should send a goodbye message on /quit");
    }

    @Test
    @DisplayName("oversized message is rejected without dropping the session")
    void messaging_oversizedMessage_isRejected() throws IOException {
        clientA = new TestClient(TEST_PORT);
        clientA.readLine();
        clientA.sendLine("Alice");
        clientA.readUntilContains("Welcome");

        // Send a message well over the 500-char limit
        clientA.sendLine("A".repeat(600));

        String response = clientA.readUntilContains("too long");
        assertTrue(response.toLowerCase().contains("too long"),
                "server should reject messages that exceed the length limit");

        // Session should still be alive — send a normal message and confirm it works
        clientA.sendLine("Still here!");
        // No exception thrown = session survived
    }

    // =========================================================================
    // TestClient — inner helper class
    // =========================================================================

    /**
     * A thin wrapper around a Socket that provides convenient line-oriented I/O
     * and a readUntilContains() helper for async server responses.
     */
    private static class TestClient {

        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;

        /**
         * Opens a connection to localhost on the given port.
         * Sets a read timeout so tests fail fast instead of hanging forever.
         */
        TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(TIMEOUT_MS);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        /** Sends a single line to the server. */
        void sendLine(String line) {
            out.println(line);
        }

        /** Reads a single line from the server. */
        String readLine() throws IOException {
            return in.readLine();
        }

        /**
         * Reads lines from the server until one contains the target substring,
         * or until the socket timeout fires. Returns the matching line.
         *
         * This is necessary because the server may send several lines
         * (e.g. a join broadcast) before the line we actually care about.
         */
        String readUntilContains(String target) throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains(target)) {
                    return line;
                }
            }
            return ""; // target never found before connection closed
        }

        /** Closes the socket quietly. */
        void close() {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException e) {
                // Nothing useful to do here.
            }
        }
    }
}