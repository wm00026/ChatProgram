package server;

import common.Message;
import common.Message.MessageType;
import common.User;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ChatRoom
 * Represents a single named chat room. Owns its member set and is
 * responsible for broadcasting to those members. 
 * 
 * Note: This is a skeleton; in Phase III, the class will be more full
 */
public class ChatRoom {

    private final String name;
    
    private String password = null;

    private final Set<String> memberNames = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, User> connectedUsers;

    /**
     * 
     * @param name
     * @param connectedUsers
     * @param password
     */
    public ChatRoom(String name, ConcurrentHashMap<String, User> connectedUsers, String password) {
        this.name = name;
        this.connectedUsers = connectedUsers;
        this.password = password;
    }

    // === Member Management ===

    public void addMember(String username) {
        memberNames.add(username);
    }

    public void removeMember(String username) {
        memberNames.remove(username);
    }

    public boolean hasMember(String username) {
        return memberNames.contains(username);
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(memberNames);
    }

    public int getMemberCount() {
        return memberNames.size();
    }

    // === Messaging ===

    /**
     * 
     * @param formatted
     * @param excludeUsername
     */
    public void broadcast(String formatted, String excludeUsername) {
        memberNames.forEach(name -> {
            if (name.equals(excludeUsername)) return;
            User u = connectedUsers.get(name);
            if (u != null && u.isActive()) {
                u.getOut().println(formatted);
            }
        });
    }

    /**
     * 
     * @param content
     * @param excludeUsername
     */
    public void broadcastSystemMessage(String content, String excludeUsername) {
        Message msg = new Message("SYSTEM", content, MessageType.SYSTEM);
        broadcast(msg.format(), excludeUsername);
    }

    // === Getters ===

    public String getName() {
        return name;
    }

    /**
     * Validates the provided password against this room's password.
     * If the room has no password, access is allowed.
     * 
     * @param password the password to check
     * @return true if access is allowed, false if password is incorrect
     */
    public boolean checkPassword(String password) {
        // If room has no password, allow access
        if (this.password == null) {
            return true;
        }
        // If room has password, require it to match
        return this.password.equals(password);
    }


    
}
