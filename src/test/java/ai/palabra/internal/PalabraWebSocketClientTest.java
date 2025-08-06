package ai.palabra.internal;

import ai.palabra.base.Message;
import ai.palabra.base.TranscriptionMessage;
import ai.palabra.base.AudioMessage;
import ai.palabra.base.TaskMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PalabraWebSocketClient.
 * These are unit tests that don't require a real WebSocket server.
 */
class PalabraWebSocketClientTest {
    private PalabraWebSocketClient client;
    private static final String TEST_URI = "ws://localhost:8080";
    private static final String TEST_TOKEN = "test-token-123";
    
    @BeforeEach
    void setUp() {
        client = new PalabraWebSocketClient(TEST_URI, TEST_TOKEN);
    }
    
    @Test
    void testClientCreation() {
        assertNotNull(client);
        assertFalse(client.isConnected());
    }
    
    @Test
    void testMessageHandlerSetting() {
        AtomicReference<Message> receivedMessage = new AtomicReference<>();
        
        client.setMessageHandler(receivedMessage::set);
        
        // Handler should be set (can't directly test private field, but no exception means success)
        assertDoesNotThrow(() -> client.setMessageHandler(message -> {}));
    }
    
    @Test
    void testErrorHandlerSetting() {
        AtomicReference<String> receivedError = new AtomicReference<>();
        
        client.setErrorHandler(receivedError::set);
        
        // Handler should be set
        assertDoesNotThrow(() -> client.setErrorHandler(error -> {}));
    }
    
    @Test
    void testConnectionHandlers() {
        AtomicBoolean connectionCalled = new AtomicBoolean(false);
        AtomicBoolean disconnectionCalled = new AtomicBoolean(false);
        
        client.setConnectionHandler(() -> connectionCalled.set(true));
        client.setDisconnectionHandler(() -> disconnectionCalled.set(true));
        
        // Handlers should be set without exception
        assertDoesNotThrow(() -> {
            client.setConnectionHandler(() -> {});
            client.setDisconnectionHandler(() -> {});
        });
    }
    
    @Test
    void testSendMessageWhenNotConnected() {
        // Should not throw exception, but queue the message
        assertDoesNotThrow(() -> {
            CompletableFuture<Void> future = client.sendMessage("test message");
            future.get(1, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testClose() {
        CompletableFuture<Void> closeFuture = client.close(3);
        
        assertDoesNotThrow(() -> closeFuture.get(5, TimeUnit.SECONDS));
        assertFalse(client.isConnected());
    }
    
    @Test
    void testGetNextMessageWhenEmpty() {
        Message message = client.getNextMessage();
        assertNull(message);
    }
    
    @Test
    @Timeout(10)
    void testConnectionToInvalidServer() {
        // This should fail quickly for an invalid server
        PalabraWebSocketClient invalidClient = new PalabraWebSocketClient("ws://invalid-server:9999", "token");
        
        CompletableFuture<Void> connectionFuture = invalidClient.connect();
        
        assertThrows(Exception.class, () -> {
            connectionFuture.get(5, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testUriConstruction() {
        // Test that URI is properly constructed with token
        PalabraWebSocketClient testClient = new PalabraWebSocketClient("ws://test.com", "my-token");
        
        // Can't directly access private uri field, but construction should not throw
        assertNotNull(testClient);
    }
    
    @Test
    void testMultipleHandlerSettings() {
        // Test that handlers can be overwritten
        client.setMessageHandler(message -> {});
        client.setErrorHandler(error -> {});
        client.setConnectionHandler(() -> {});
        client.setDisconnectionHandler(() -> {});
        
        // Overwrite with new handlers
        assertDoesNotThrow(() -> {
            client.setMessageHandler(message -> {});
            client.setErrorHandler(error -> {});
            client.setConnectionHandler(() -> {});
            client.setDisconnectionHandler(() -> {});
        });
    }
    
    @Test
    void testSendMultipleMessages() {
        // Test sending multiple messages when not connected
        assertDoesNotThrow(() -> {
            CompletableFuture<Void> future1 = client.sendMessage("message 1");
            CompletableFuture<Void> future2 = client.sendMessage("message 2");
            CompletableFuture<Void> future3 = client.sendMessage("message 3");
            
            CompletableFuture.allOf(future1, future2, future3).get(2, TimeUnit.SECONDS);
        });
    }
}
