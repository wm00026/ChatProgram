package common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;


import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Testing for Message Class
 * Three main sections:
 * - Construction: getters, settings, timestamps
 * - format(): output string for each MessageType
 * - Edge caeses: empty content, long content, ordering
 */

public class MessageTest {


    private static final String SENDER    = "Alice";
    private static final String RECIPIENT = "Bob";
    private static final String CONTENT   = "Hello, world!";
 
    // === Construction Tests === 
 
    @Test
    @DisplayName("stores sender correctly")
    void constructor_storesSender() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PUBLIC);
        assertEquals(SENDER, msg.getSender());
    }
 
    @Test
    @DisplayName("stores content correctly")
    void constructor_storesContent() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PUBLIC);
        assertEquals(CONTENT, msg.getContent());
    }
 
    @Test
    @DisplayName("stores type correctly")
    void constructor_storesType() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.SYSTEM);
        assertEquals(Message.MessageType.SYSTEM, msg.getType());
    }
 
    @Test
    @DisplayName("recipient is null when using the three-arg constructor")
    void constructor_noRecipient_recipientIsNull() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PUBLIC);
        assertNull(msg.getRecipient());
    }
 
    @Test
    @DisplayName("stores recipient when provided")
    void constructor_storesRecipient() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PRIVATE, RECIPIENT);
        assertEquals(RECIPIENT, msg.getRecipient());
    }
 
    @Test
    @DisplayName("timestamp is set at construction time and is not null")
    void constructor_timestampIsSet() {
        LocalDateTime before = LocalDateTime.now();
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PUBLIC);
        LocalDateTime after = LocalDateTime.now();
 
        assertNotNull(msg.getTimestamp());
        assertFalse(msg.getTimestamp().isBefore(before), "timestamp should not be before construction");
        assertFalse(msg.getTimestamp().isAfter(after),   "timestamp should not be after construction");
    }
 
    // === format() tests
 
    @Test
    @DisplayName("PUBLIC format includes sender and content")
    void format_public_includesSenderAndContent() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PUBLIC);
        String formatted = msg.format();
        assertAll(
            () -> assertTrue(formatted.contains(SENDER),  "should contain sender"),
            () -> assertTrue(formatted.contains(CONTENT), "should contain content")
        );
    }
 
    @Test
    @DisplayName("PUBLIC format matches [HH:mm:ss] sender: content pattern")
    void format_public_matchesPattern() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PUBLIC);
        assertTrue(msg.format().matches("\\[\\d{2}:\\d{2}:\\d{2}\\] Alice: Hello, world!"));
    }
 
    @Test
    @DisplayName("PRIVATE format includes 'PM from', sender, and content")
    void format_private_includesAllParts() {
        Message msg = new Message(SENDER, CONTENT, Message.MessageType.PRIVATE, RECIPIENT);
        String formatted = msg.format();
        assertAll(
            () -> assertTrue(formatted.contains("PM from"), "should say 'PM from'"),
            () -> assertTrue(formatted.contains(SENDER),    "should contain sender"),
            () -> assertTrue(formatted.contains(CONTENT),   "should contain content")
        );
    }
 
    @Test
    @DisplayName("SYSTEM format includes 'SYSTEM' label and content")
    void format_system_includesLabel() {
        Message msg = new Message("SYSTEM", CONTENT, Message.MessageType.SYSTEM);
        String formatted = msg.format();
        assertAll(
            () -> assertTrue(formatted.contains("SYSTEM"), "should contain SYSTEM label"),
            () -> assertTrue(formatted.contains(CONTENT),  "should contain content")
        );
    }
 
    @Test
    @DisplayName("SYSTEM format does not render as 'SYSTEM: content'")
    void format_system_omitsSenderColon() {
        Message msg = new Message("SYSTEM", "Server restarting", Message.MessageType.SYSTEM);
        assertFalse(msg.format().matches(".*SYSTEM: .*"),
            "SYSTEM type should use [SYSTEM] label, not 'SYSTEM: ...'");
    }
 
    @Test
    @DisplayName("JOIN format contains the join content")
    void format_join_containsContent() {
        String joinContent = Protocol.joinMessage("Dave");
        Message msg = new Message("SYSTEM", joinContent, Message.MessageType.JOIN);
        assertTrue(msg.format().contains(joinContent));
    }
 
    @Test
    @DisplayName("LEAVE format contains the leave content")
    void format_leave_containsContent() {
        String leaveContent = Protocol.leaveMessage("Dave");
        Message msg = new Message("SYSTEM", leaveContent, Message.MessageType.LEAVE);
        assertTrue(msg.format().contains(leaveContent));
    }
 
    @Test
    @DisplayName("ERROR format falls through to default and includes sender and content")
    void format_error_includesSenderAndContent() {
        Message msg = new Message(SENDER, "Something went wrong", Message.MessageType.ERROR);
        String formatted = msg.format();
        assertAll(
            () -> assertTrue(formatted.contains(SENDER),                "should contain sender"),
            () -> assertTrue(formatted.contains("Something went wrong"), "should contain content")
        );
    }
 
    // === Edge cases tests ===
 
    @Test
    @DisplayName("format() handles empty content without throwing")
    void format_emptyContent_doesNotThrow() {
        Message msg = new Message(SENDER, "", Message.MessageType.PUBLIC);
        assertDoesNotThrow(msg::format);
    }
 
    @Test
    @DisplayName("format() handles very long content without throwing")
    void format_longContent_doesNotThrow() {
        String longContent = "x".repeat(10_000);
        Message msg = new Message(SENDER, longContent, Message.MessageType.PUBLIC);
        assertDoesNotThrow(msg::format);
    }
 
    @Test
    @DisplayName("format() always starts with a timestamp bracket for every type")
    void format_alwaysStartsWithTimestamp() {
        for (Message.MessageType type : Message.MessageType.values()) {
            Message msg = new Message(SENDER, CONTENT, type);
            assertTrue(msg.format().startsWith("["),
                "format() for " + type + " should start with '['");
        }
    }
 
    @Test
    @DisplayName("two messages constructed back-to-back have non-decreasing timestamps")
    void timestamps_areNonDecreasing() {
        Message first  = new Message(SENDER, "first",  Message.MessageType.PUBLIC);
        Message second = new Message(SENDER, "second", Message.MessageType.PUBLIC);
        assertFalse(second.getTimestamp().isBefore(first.getTimestamp()),
            "second message timestamp should not be before the first");
    }
    
}
