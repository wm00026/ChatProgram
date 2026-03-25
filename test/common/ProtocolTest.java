package common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;


/**
 * Testing for Protocol Class
 * * Four major sections:
 * - isValidUsername
 * - isComannd
 * - parseWhisperMessage
 * - message formatters
 */

public class ProtocolTest {
 // === isValidUsername Tests ===

    @Test
    @DisplayName("accepts a normal alphanumeric username")
    void username_acceptsAlphanumeric() {
        assertTrue(Protocol.isValidUsername("John123"));
    }

    @Test
    @DisplayName("accepts a username with underscores")
    void username_acceptsUnderscores() {
        assertTrue(Protocol.isValidUsername("cool_user"));
    }

    @Test
    @DisplayName("accepts exactly MIN_USERNAME_LENGTH chars")
    void username_acceptsMinLength() {
        assertTrue(Protocol.isValidUsername("abc"));
    }

    @Test
    @DisplayName("accepts exactly MAX_USERNAME_LENGTH chars")
    void username_acceptsMaxLength() {
        assertTrue(Protocol.isValidUsername("A".repeat(Protocol.MAX_USERNAME_LENGTH)));
    }

    @Test
    @DisplayName("rejects username one character below minimum")
    void username_rejectsBelowMinLength() {
        assertFalse(Protocol.isValidUsername("ab"));
    }

    @Test
    @DisplayName("rejects username one char above max.")
    void username_rejectsAboveMaxLength() {
        assertFalse(Protocol.isValidUsername("A".repeat(Protocol.MAX_USERNAME_LENGTH + 1)));
    }

    @NullAndEmptySource
    @ParameterizedTest(name = "rejects null and empty: [{0}]")
    void username_rejectsNullAndEmpty(String input) {
        assertFalse(Protocol.isValidUsername(input));
    }

    @Test
    @DisplayName("rejects whitespace-only username")
    void username_rejectsWhitespaceOnly() {
        assertFalse(Protocol.isValidUsername("   "));
    }

    @Test
    @DisplayName("trims leading and trailing whitespace before validating")
    void username_trimmed() {
        assertTrue(Protocol.isValidUsername("  ALICE  "));
    }

    @ValueSource(strings = {"user name", "user@name", "user-name", "user.name", "user!name"})
    @ParameterizedTest(name = "rejects invalid character in: [{0}]")
    void username_rejectsSpecialCharacters(String input) {
        assertFalse(Protocol.isValidUsername(input));
    }
    

    // === isCommand Tests ===

    @Test
    @DisplayName("recognizes /list as a command")
    void isCommand_list() {
        assertTrue(Protocol.isCommand(Protocol.CMD_LIST_USERS));
    }

    @Test
    @DisplayName("recognises /quit as a command")
    void isCommand_quit() {
        assertTrue(Protocol.isCommand(Protocol.CMD_LEAVE));
    }

    @Test
    @DisplayName("recognizes /whisper as a command")
    void isCommand_whisper() {
        assertTrue(Protocol.isCommand(Protocol.CMD_WHISPER));
    }

    @Test
    @DisplayName("recognizes /help as a command")
    void isCommand_help() {
        assertTrue(Protocol.isCommand(Protocol.CMD_HELP));
    }

    @Test
    @DisplayName("does not recognizes a normal message as a command")
    void isCommand_normalMessage() {
        assertFalse(Protocol.isCommand("hello everyone"));
    }

    @Test
    @DisplayName("returns false for null input")
    void isCommand_null() {
        assertFalse(Protocol.isCommand(null));
    }

    @Test
    @DisplayName("returns false for empty string")
    void isCommand_empty() {
        assertFalse(Protocol.isCommand(""));
    }

    // === parseWhisperCommand Tests ===

    @Test
    @DisplayName("parses a well-formed whisper into recipient and messasge")
    void parseWhisper_happyPath() {
        String[] result = Protocol.parseWhisperCommand("/whisper Bob Hello Bob!");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("Bob", result[0]);
        assertEquals("Hello Bob!", result[1]);
    }

    @Test
    @DisplayName("parses a multi-word whisper message correctly")
    void parseWhisper_multiWordMessage() {
        String[] result = Protocol.parseWhisperCommand("/whisper Alice how are you doing today");
        assertNotNull(result);
        assertEquals("Alice", result[0]);
        assertEquals("how are you doing today", result[1]);
    }

    @Test
    @DisplayName("returns null when no message body is provided")
    void parseWhisper_noMessage() {
        assertNull(Protocol.parseWhisperCommand("/whisper Bob"));
    }

    @Test
    @DisplayName("returns null when no recipient is provided")
    void parseWhisper_commandOnly() {
        assertNull(Protocol.parseWhisperCommand("/whisper"));
    }

    @Test
    @DisplayName("returns null for null input")
    void parseWhisper_null() {
        assertNull(Protocol.parseWhisperCommand(null));
    }

    @Test
    @DisplayName("returns null when input does not start with /whisper")
    void parseWhisper_wrongCommand() {
        assertNull(Protocol.parseWhisperCommand("/list"));
    }

    @Test
    @DisplayName("returns null when recipient is empty after trimming")
    void parseWhisper_emptyRecipient() {
        assertNull(Protocol.parseWhisperCommand("/whisper  "));
    }

    // === Message formatter tests ===

    @Test
    @DisplayName("standardMessage formats as [username] message")
    void standardMessage_format() {
        String result = Protocol.standardMessage("alice", "Hello!");
        assertEquals("[alice] Hello!", result);
    }

    @Test
    @DisplayName("joinMessage wraps the username in a join notification")
    void joinMessage_format() {
        String result = Protocol.joinMessage("Bob");
        assertEquals("[Bob has joined!]", result);
    }
 
    @Test
    @DisplayName("leaveMessage wraps the username in a leave notification")
    void leaveMessage_format() {
        String result = Protocol.leaveMessage("Charlie");
        assertEquals("[Charlie has left]", result);
    }
 
    @Test
    @DisplayName("errorMessage returns a non-blank string")
    void errorMessage_notBlank() {
        assertNotNull(Protocol.errorMessage());
        assertFalse(Protocol.errorMessage().isBlank());
    }
 
    @Test
    @DisplayName("whisperMessage includes sender, recipient, and content")
    void whisperMessage_containsAllParts() {
        String result = Protocol.whisperMessage("Alice", "Bob", "secret");
        assertAll(
            () -> assertTrue(result.contains("Alice"),  "should contain sender"),
            () -> assertTrue(result.contains("Bob"),    "should contain recipient"),
            () -> assertTrue(result.contains("secret"), "should contain message")
        );
    }
 
    @Test
    @DisplayName("whisperReceivedMessage includes sender and content")
    void whisperReceivedMessage_containsAllParts() {
        String result = Protocol.whisperReceivedMessage("Alice", "secret");
        assertAll(
            () -> assertTrue(result.contains("Alice"),  "should contain sender"),
            () -> assertTrue(result.contains("secret"), "should contain message")
        );
    }
 
    @Test
    @DisplayName("getHelpMessage lists all four commands")
    void helpMessage_containsAllCommands() {
        String help = Protocol.getHelpMessage();
        assertAll(
            () -> assertTrue(help.contains(Protocol.CMD_LIST_USERS), "should list /list"),
            () -> assertTrue(help.contains(Protocol.CMD_LEAVE),      "should list /quit"),
            () -> assertTrue(help.contains(Protocol.CMD_WHISPER),    "should list /whisper"),
            () -> assertTrue(help.contains(Protocol.CMD_HELP),       "should list /help")
        );
    }
}
