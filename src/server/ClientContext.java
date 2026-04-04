package server;

import common.User;
import java.io.PrintWriter;

/**
 * 
 */
public class ClientContext {
    
    private final String username;
    private final PrintWriter out;
    private ChatRoom currentRoom;
    private final User user;

    public ClientContext(String username, PrintWriter out, ChatRoom currentRoom, User user) {
        this.username = username;
        this.out = out;
        this.currentRoom = currentRoom;
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public PrintWriter getOut() {
        return out;
    }

    public User getUser() {
        return user;
    }

    public ChatRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(ChatRoom room) { this.currentRoom = room; }

}
