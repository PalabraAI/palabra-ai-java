package ai.palabra.internal;

import ai.palabra.PalabraException;
import ai.palabra.base.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * WebSocket client for real-time communication with Palabra AI API.
 * Handles connection management, message sending/receiving, and error recovery.
 * 
 * Based on the Python library's WebSocketClient implementation.
 */
public class PalabraWebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(PalabraWebSocketClient.class);
    private static final Duration PING_INTERVAL = Duration.ofSeconds(10);
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(1);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    
    private final ObjectMapper objectMapper;
    private final String uri;
    private final String token;
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Object> outgoingMessages = new ConcurrentLinkedQueue<>();
    
    private WebSocketClient websocket;
    private Consumer<Message> messageHandler;
    private Consumer<String> errorHandler;
    private Runnable connectionHandler;
    private Runnable disconnectionHandler;
    private int reconnectAttempts = 0;
    
    /**
     * Creates a new WebSocket client.
     * 
     * @param uri The WebSocket URI
     * @param token The authentication token
     */
    public PalabraWebSocketClient(String uri, String token) {
        this.uri = uri + "?token=" + token;
        this.token = token;
        this.objectMapper = new ObjectMapper();
        // Configure Jackson to not escape non-ASCII characters
        this.objectMapper.getFactory().configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }
    
    /**
     * Sets the message handler for incoming messages.
     * 
     * @param handler The message handler
     */
    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }
    
    /**
     * Sets the error handler for WebSocket errors.
     * 
     * @param handler The error handler
     */
    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }
    
    /**
     * Sets the connection handler.
     * 
     * @param handler The connection handler
     */
    public void setConnectionHandler(Runnable handler) {
        this.connectionHandler = handler;
    }
    
    /**
     * Sets the disconnection handler.
     * 
     * @param handler The disconnection handler
     */
    public void setDisconnectionHandler(Runnable handler) {
        this.disconnectionHandler = handler;
    }
    
    /**
     * Connects to the WebSocket server.
     * 
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> connectionFuture = new CompletableFuture<>();
        
        try {
            URI websocketUri = URI.create(uri);
            
            websocket = new WebSocketClient(websocketUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    logger.info("WebSocket connected to {}", getURI());
                    reconnectAttempts = 0;
                    
                    if (connectionHandler != null) {
                        connectionHandler.run();
                    }
                    
                    // Start message processing
                    startMessageProcessing();
                    
                    if (!connectionFuture.isDone()) {
                        connectionFuture.complete(null);
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    // Always log that we received something
                    logger.info("RAW MESSAGE RECEIVED: {} bytes", message.length());
                    
                    // Don't log input audio data messages to keep logs clean, but show output audio
                    if (message.contains("\"message_type\":\"input_audio_data\"")) {
                        logger.debug("INPUT AUDIO message ({} bytes)", message.length());
                    } else if (message.contains("\"message_type\":\"output_audio_data\"")) {
                        logger.debug("OUTPUT AUDIO message ({} bytes)", message.length());
                    } else if (message.contains("\"message_type\":\"partial_transcription\"")) {
                        logger.debug("PARTIAL TRANSCRIPTION: {}", message);
                    } else if (message.contains("\"message_type\":\"final_transcription\"")) {
                        logger.debug("FINAL TRANSCRIPTION: {}", message);
                    } else if (message.contains("\"message_type\":\"current_task\"")) {
                        logger.debug("TASK STATUS: {}", message);
                    } else {
                        logger.info("OTHER MESSAGE: {}", message.length() > 800 ? message.substring(0, 800) + "..." : message);
                    }
                    handleIncomingMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("WebSocket connection closed: {} - {}", code, reason);
                    
                    if (disconnectionHandler != null) {
                        disconnectionHandler.run();
                    }
                    
                    // Attempt reconnection if still running
                    if (keepRunning.get() && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect();
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    logger.error("WebSocket error: {}", ex.getMessage(), ex);
                    
                    if (errorHandler != null) {
                        errorHandler.accept(ex.getMessage());
                    }
                    
                    if (!connectionFuture.isDone()) {
                        connectionFuture.completeExceptionally(new PalabraException("WebSocket connection failed", ex));
                    }
                }
            };
            
            // Set connection timeout
            websocket.setConnectionLostTimeout(60);
            
            // Connect
            websocket.connect();
            
        } catch (Exception e) {
            connectionFuture.completeExceptionally(new PalabraException("Failed to create WebSocket connection", e));
        }
        
        return connectionFuture;
    }
    
    /**
     * Sends a message through the WebSocket.
     * 
     * @param message The message to send
     * @return CompletableFuture that completes when message is sent
     */
    public CompletableFuture<Void> sendMessage(Object message) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (websocket != null && websocket.isOpen()) {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    websocket.send(jsonMessage);
                    
                    // Don't log input audio data messages to keep logs clean, but show output audio
                    if (jsonMessage.contains("\"message_type\":\"input_audio_data\"")) {
                        logger.debug("Sent input audio data message ({} bytes)", jsonMessage.length());
                    } else if (jsonMessage.contains("\"message_type\":\"output_audio_data\"")) {
                        logger.info("🔊 Sent OUTPUT audio data message ({} bytes)", jsonMessage.length());
                    } else {
                        logger.debug("Sent WebSocket message: {}", jsonMessage);
                    }
                } else {
                    // Queue message for later sending
                    outgoingMessages.offer(message);
                    logger.debug("WebSocket not connected, queued message for later");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to send WebSocket message", e);
            }
        });
    }
    
    /**
     * Closes the WebSocket connection.
     * 
     * @param waitSeconds Maximum time to wait for graceful close
     * @return CompletableFuture that completes when closed
     */
    public CompletableFuture<Void> close(int waitSeconds) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Closing WebSocket connection");
            keepRunning.set(false);
            
            if (websocket != null) {
                try {
                    websocket.closeBlocking();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while closing WebSocket");
                }
            }
        });
    }
    
    /**
     * Checks if the WebSocket is currently connected.
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return websocket != null && websocket.isOpen();
    }
    
    /**
     * Gets the next incoming message from the queue.
     * 
     * @return The next message, or null if none available
     */
    public Message getNextMessage() {
        return incomingMessages.poll();
    }
    
    private void handleIncomingMessage(String rawMessage) {
        try {
            // Parse the message similar to Python implementation
            Object messageData = objectMapper.readValue(rawMessage, Object.class);
            
            // Create a Message object (will be implemented with proper message types)
            Message message = Message.fromRawData(messageData);
            
            // Add to queue for processing
            incomingMessages.offer(message);
            
            // Call handler if set
            if (messageHandler != null) {
                messageHandler.accept(message);
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse incoming WebSocket message: {}", rawMessage, e);
        }
    }
    
    private void startMessageProcessing() {
        // Process queued outgoing messages
        CompletableFuture.runAsync(() -> {
            while (keepRunning.get() && websocket != null && websocket.isOpen()) {
                Object message = outgoingMessages.poll();
                if (message != null) {
                    try {
                        String jsonMessage = objectMapper.writeValueAsString(message);
                        websocket.send(jsonMessage);
                        logger.debug("Sent queued message: {}", jsonMessage);
                    } catch (Exception e) {
                        logger.error("Failed to send queued message", e);
                    }
                }
                
                try {
                    Thread.sleep(10); // Small delay to prevent busy waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void scheduleReconnect() {
        reconnectAttempts++;
        logger.info("Scheduling reconnection attempt {} in {} seconds", 
                   reconnectAttempts, RECONNECT_DELAY.getSeconds());
        
        CompletableFuture.delayedExecutor(RECONNECT_DELAY.toMillis(), TimeUnit.MILLISECONDS)
            .execute(() -> {
                if (keepRunning.get()) {
                    logger.info("Attempting to reconnect WebSocket");
                    connect().exceptionally(ex -> {
                        logger.error("Reconnection failed: {}", ex.getMessage());
                        return null;
                    });
                }
            });
    }
}
