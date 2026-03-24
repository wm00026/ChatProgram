package common;

import java.net.Socket;
import java.io.PrintWriter;


/**
 * User class
 * Sets username, socket used for connection, the PrintWriter,
 * and active status.
 */
public class User {
    private String username;
    private Socket socket;
    private PrintWriter out;
    private boolean isActive;

    /**
     * Constructor for the User Class
     * @param username the name of the user
     * @param socket the socket used for connection
     * @param out the PrintWriter out
     */
    public User(String username, Socket socket, PrintWriter out) {
        this.username = username;
        this.socket = socket;
        this.out = out;
        this.isActive = true;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public Socket getSocket() { return socket; }
    public PrintWriter getOut() { return out; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
    }
}
