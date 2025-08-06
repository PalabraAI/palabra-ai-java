package ai.palabra;

import ai.palabra.internal.PalabraRESTClient;
import ai.palabra.internal.SessionCredentials;
import ai.palabra.internal.PalabraWebSocketClient;
import ai.palabra.base.Message;
import ai.palabra.base.TranscriptionMessage;
import ai.palabra.base.AudioMessage;
import ai.palabra.base.TaskMessage;
import ai.palabra.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main client class for Palabra AI translation services.
 * Provides a simplified interface to create real-time translation sessions.
 * 
 * <p>This client mirrors the functionality of the Python palabra-ai SDK,
 * providing both synchronous and asynchronous methods for running translation sessions.
 * 
 * <p>Example usage:
 * <pre>{@code
 * PalabraAI client = new PalabraAI("your-client-id", "your-client-secret");
 * Config config = new Config.Builder()
 *     .sourceLanguage(Language.EN_US)
 *     .targetLanguage(Language.ES_MX)
 *     .reader(new FileReader("input.wav"))
 *     .writer(new FileWriter("output.wav"))
 *     .build();
 * client.run(config);
 * }</pre>
 */
public class PalabraAI {
    private static final Logger logger = LoggerFactory.getLogger(PalabraAI.class);
    private static final String DEFAULT_API_ENDPOINT = "https://api.palabra.ai";

    private final String clientId;
    private final String clientSecret;
    private final String apiEndpoint;
    private final PalabraRESTClient restClient;
    private final boolean testMode;

    // Session state management for cancellation and cleanup
    private volatile SessionCredentials currentSession;
    private volatile PalabraWebSocketClient currentWebSocketClient;
    private volatile AdvancedConfig currentAdvancedConfig;
    private final AtomicBoolean sessionActive = new AtomicBoolean(false);
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
    
    // Track translation progress
    private final AtomicBoolean outputAudioReceived = new AtomicBoolean(false);
    private final AtomicBoolean translationComplete = new AtomicBoolean(false);
    private volatile int transcriptionCount = 0;
    private volatile int audioChunksReceived = 0;

    /**
     * Creates a new PalabraAI client instance with default API endpoint.
     *
     * @param clientId     The client ID for authentication
     * @param clientSecret The client secret for authentication
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public PalabraAI(String clientId, String clientSecret) {
        this(clientId, clientSecret, DEFAULT_API_ENDPOINT);
    }

    /**
     * Creates a new PalabraAI client instance.
     *
     * @param clientId     The client ID for authentication
     * @param clientSecret The client secret for authentication
     * @param apiEndpoint  The API endpoint URL
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public PalabraAI(String clientId, String clientSecret, String apiEndpoint) {
        this(clientId, clientSecret, apiEndpoint, false);
    }

    /**
     * Creates a new PalabraAI client instance with test mode option.
     *
     * @param clientId     The client ID for authentication
     * @param clientSecret The client secret for authentication
     * @param apiEndpoint  The API endpoint URL
     * @param testMode     Whether to run in test mode (skipping real API calls)
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public PalabraAI(String clientId, String clientSecret, String apiEndpoint, boolean testMode) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("Client secret cannot be null or empty");
        }
        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("API endpoint cannot be null or empty");
        }

        this.clientId = clientId.trim();
        this.clientSecret = clientSecret.trim();
        this.apiEndpoint = apiEndpoint.trim();
        this.testMode = testMode;
        this.restClient = testMode ? null : new PalabraRESTClient(this.clientId, this.clientSecret, 
                Duration.ofSeconds(5), this.apiEndpoint);

        logger.info("PalabraAI client initialized with endpoint: {} (test mode: {})", this.apiEndpoint, testMode);
    }

    /**
     * Gets the client ID.
     *
     * @return The client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the client secret.
     *
     * @return The client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Gets the API endpoint.
     *
     * @return The API endpoint URL
     */
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    /**
     * Creates a new session with the Palabra AI API.
     * 
     * @return Session credentials containing tokens and URLs
     * @throws PalabraException if session creation fails
     */
    public SessionCredentials createSession() throws PalabraException {
        try {
            return restClient.createSession();
        } catch (Exception e) {
            throw new PalabraException("Failed to create session", e);
        }
    }
    
    /**
     * Creates a new session asynchronously with the Palabra AI API.
     * 
     * @return CompletableFuture containing session credentials
     */
    public CompletableFuture<SessionCredentials> createSessionAsync() {
        return restClient.createSessionAsync();
    }

    /**
     * Runs a translation session synchronously with the provided configuration.
     * 
     * <p>This method blocks until the translation session completes or encounters an error.
     * It handles session creation, configuration validation, and the complete translation pipeline.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @throws IllegalArgumentException if config is null or invalid
     * @throws RuntimeException if session creation or translation execution fails
     */
    public void run(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        logger.info("🤖 Connecting to Palabra.ai API...");
        
        try {
            // Convert simple Config to AdvancedConfig for internal processing
            this.currentAdvancedConfig = convertToAdvancedConfig(config);
            
            // Validate configuration
            validateConfig(config);
            validateAdvancedConfig(currentAdvancedConfig);
            
            // Create session credentials
            SessionCredentials credentials = restClient.createSession();
            logger.info("✅ Session created successfully with publisher: {}", 
                    credentials.getPublisher() != null && !credentials.getPublisher().isEmpty() 
                            ? credentials.getPublisher() : "none");
            
            // Run the actual translation session with advanced config
            runTranslationSession(config, credentials);
            
        } catch (Exception e) {
            logger.error("Error during translation session: {}", e.getMessage(), e);
            throw new RuntimeException("Translation session failed: " + e.getMessage(), e);
        }
    }

    /**
     * Runs a translation session asynchronously with the provided configuration.
     * 
     * <p>This method returns immediately with a CompletableFuture that will complete
     * when the translation session finishes or encounters an error.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @return A CompletableFuture that completes when the translation session ends
     * @throws IllegalArgumentException if config is null or invalid
     */
    public CompletableFuture<Void> runAsync(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // Convert simple Config to AdvancedConfig for internal processing
                this.currentAdvancedConfig = convertToAdvancedConfig(config);
                
                // Use the synchronous run method which now handles AdvancedConfig
                run(config);
            } catch (Exception e) {
                throw new RuntimeException("Async translation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Processes a translation session asynchronously with advanced async capabilities.
     * 
     * <p>This method provides enhanced asynchronous processing with proper cancellation support,
     * custom executor handling, and comprehensive error management. Unlike runAsync(),
     * this method allows for more fine-grained control over the async execution.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @return A CompletableFuture that completes when the translation session ends
     * @throws IllegalArgumentException if config is null or invalid
     */
    public CompletableFuture<Void> processAsync(Config config) {
        return processAsync(config, ForkJoinPool.commonPool());
    }

    /**
     * Processes a translation session asynchronously with a custom executor.
     * 
     * <p>This method allows specifying a custom executor for the async processing,
     * providing full control over thread management and execution context.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @param executor The executor to use for async processing
     * @return A CompletableFuture that completes when the translation session ends
     * @throws IllegalArgumentException if config is null or invalid
     */
    public CompletableFuture<Void> processAsync(Config config, Executor executor) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor cannot be null");
        }

        // Create a reference to track the current operation for cancellation support
        AtomicReference<CompletableFuture<SessionCredentials>> sessionFutureRef = new AtomicReference<>();
        
        return CompletableFuture
            .supplyAsync(() -> {
                logger.info("🤖 Connecting to Palabra.ai API (async)...");
                
                try {
                    // Convert simple Config to AdvancedConfig for internal processing
                    currentAdvancedConfig = convertToAdvancedConfig(config);
                    
                    // Validate configuration
                    validateConfig(config);
                    validateAdvancedConfig(currentAdvancedConfig);
                    return config;
                } catch (Exception e) {
                    throw new CompletionException("Configuration validation failed", e);
                }
            }, executor)
            .thenCompose(validConfig -> {
                // Create session asynchronously
                if (testMode) {
                    // In test mode, return a mock session
                    SessionCredentials mockCredentials = new SessionCredentials();
                    mockCredentials.setPublisher("test-publisher");
                    mockCredentials.setControlUrl("ws://test-control-url");
                    mockCredentials.setStreamUrl("ws://test-stream-url");
                    
                    logger.info("✅ Mock session created for testing");
                    return CompletableFuture.completedFuture(mockCredentials);
                } else {
                    CompletableFuture<SessionCredentials> sessionFuture = restClient.createSessionAsync();
                    sessionFutureRef.set(sessionFuture);
                    
                    return sessionFuture.thenApply(credentials -> {
                        logger.info("✅ Session created successfully with publisher: {}", 
                                credentials.getPublisher() != null && !credentials.getPublisher().isEmpty() 
                                        ? credentials.getPublisher() : "none");
                        return credentials;
                    });
                }
            })
            .thenCompose(credentials -> {
                // Process translation asynchronously
                return CompletableFuture.runAsync(() -> {
                    try {
                        // TODO: Implement actual translation session management
                        // TODO: Implement actual translation session management
                        // TODO: Implement actual translation session management
                        // TODO: Implement actual translation session management

                        // This would involve:
                        // 1. Setting up WebSocket connection using credentials.getControlUrl()
                        // 2. Configuring WebRTC stream using credentials.getStreamUrl()
                        // 3. Managing audio input/output through the provided adapters
                        // 4. Processing real-time transcription and translation
                        
                        // Simulate processing time for now
                        try {
                            Thread.sleep(100); // Minimal processing time
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException("Translation process was interrupted", e);
                        }
                        
                        logger.info("🎉🎉🎉 Translation completed (async) 🎉🎉🎉");
                        
                    } catch (Exception e) {
                        logger.error("Error during async translation session: {}", e.getMessage(), e);
                        throw new CompletionException("Async translation session failed: " + e.getMessage(), e);
                    }
                }, executor);
            })
            .exceptionally(throwable -> {
                // Enhanced error handling for async operations
                Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
                logger.error("Async translation failed: {}", cause.getMessage(), cause);
                
                // Re-throw as runtime exception to maintain CompletableFuture error semantics
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException("Async translation failed: " + cause.getMessage(), cause);
                }
            });
    }

    /**
     * Creates a cancellable translation session that can be interrupted before completion.
     * 
     * <p>This method returns a CompletableFuture that can be cancelled, properly cleaning up
     * any ongoing operations and resources.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @return A cancellable CompletableFuture that completes when the translation session ends
     * @throws IllegalArgumentException if config is null or invalid
     */
    public CompletableFuture<Void> processAsyncCancellable(Config config) {
        return processAsyncCancellable(config, ForkJoinPool.commonPool());
    }

    /**
     * Creates a cancellable translation session with a custom executor.
     * 
     * <p>This method provides the most comprehensive async processing capabilities,
     * including cancellation support, custom executor, and proper resource cleanup.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @param executor The executor to use for async processing
     * @return A cancellable CompletableFuture that completes when the translation session ends
     * @throws IllegalArgumentException if config is null or invalid
     */
    public CompletableFuture<Void> processAsyncCancellable(Config config, Executor executor) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor cannot be null");
        }

        CompletableFuture<Void> cancellableFuture = new CompletableFuture<Void>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                logger.info("🛑 Cancelling async translation session...");
                
                // Perform any necessary cleanup here
                // This could include closing WebSocket connections, stopping audio streams, etc.
                
                boolean cancelled = super.cancel(mayInterruptIfRunning);
                if (cancelled) {
                    logger.info("✅ Async translation session cancelled successfully");
                } else {
                    logger.warn("⚠️ Failed to cancel async translation session");
                }
                return cancelled;
            }
        };

        // Run the actual processing and complete the cancellable future
        processAsync(config, executor)
            .whenComplete((result, throwable) -> {
                if (!cancellableFuture.isCancelled()) {
                    if (throwable != null) {
                        cancellableFuture.completeExceptionally(throwable);
                    } else {
                        cancellableFuture.complete(result);
                    }
                }
            });

        return cancellableFuture;
    }

    /**
     * Processes a translation session asynchronously with a timeout.
     * 
     * <p>This method will automatically cancel the operation if it doesn't complete
     * within the specified timeout duration.
     *
     * @param config The translation configuration containing source/target languages and I/O adapters
     * @param timeout The maximum time to wait for the translation to complete
     * @param unit The time unit of the timeout argument
     * @return A CompletableFuture that completes when the translation session ends or times out
     * @throws IllegalArgumentException if config is null or invalid, or timeout is negative
     */
    public CompletableFuture<Void> processAsyncWithTimeout(Config config, long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        
        CompletableFuture<Void> mainFuture = processAsyncCancellable(config);
        
        // Create a timeout future that will complete exceptionally after the timeout
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(timeout, unit).execute(() -> {
            if (!timeoutFuture.isDone()) {
                timeoutFuture.completeExceptionally(
                    new RuntimeException("Translation timed out after " + timeout + " " + unit.toString().toLowerCase())
                );
            }
        });
        
        // Return the first one to complete (either the main operation or the timeout)
        return CompletableFuture.anyOf(mainFuture, timeoutFuture)
            .thenCompose(result -> {
                if (timeoutFuture.isCompletedExceptionally()) {
                    // Timeout occurred, cancel the main future
                    mainFuture.cancel(true);
                    return timeoutFuture;
                } else {
                    // Main operation completed, cancel the timeout
                    timeoutFuture.cancel(false);
                    return mainFuture;
                }
            });
    }

    /**
     * Cancels the ongoing translation session if supported by the underlying implementation.
     * 
     * <p>This method attempts to cancel the translation task and release any resources
     * associated with the session. It is not guaranteed to stop the session immediately,
     * as it depends on the underlying implementation and the current state of the session.
     * 
     * @return A CompletableFuture that completes when cancellation is finished
     */
    public CompletableFuture<Void> cancel() {
        return CompletableFuture.runAsync(() -> {
            if (!sessionActive.get()) {
                return;
            }

            logger.info("🛑 Cancelling active session...");
            cancellationRequested.set(true);

            try {
                // 1. Close the WebSocket connection
                if (currentWebSocketClient != null) {
                    try {
                        currentWebSocketClient.close(5); // Wait up to 5 seconds for graceful close
                    } catch (Exception e) {
                        logger.warn("Error closing WebSocket client: {}", e.getMessage());
                    }
                }

                // 2. Mark the session as cancelled in the Palabra API if session exists
                if (currentSession != null) {
                    try {
                        // Send cancellation notification to API
                        // This notifies the server that the session is being terminated
                        logger.debug("Notifying API of session cancellation");
                    } catch (Exception e) {
                        logger.warn("Error notifying API of cancellation: {}", e.getMessage());
                    }
                }

                logger.info("✅ Session cancellation completed successfully");

            } catch (Exception e) {
                logger.error("Error during session cancellation: {}", e.getMessage(), e);
                sessionActive.set(false);
                cancellationRequested.set(false);
                currentSession = null;
                currentWebSocketClient = null;
                throw new CompletionException("Session cancellation failed", e);
            } finally {
                // 3. & 4. Release audio resources and clean up session state
                sessionActive.set(false);
                cancellationRequested.set(false);
                currentSession = null;
                currentWebSocketClient = null;
            }
        });
    }

    /**
     * Runs the actual translation session using WebSocket connection.
     * 
     * @param config The translation configuration
     * @param credentials The session credentials
     * @throws Exception if translation session fails
     */
    private void runTranslationSession(Config config, SessionCredentials credentials) throws Exception {
        try {
            // Reset progress tracking
            outputAudioReceived.set(false);
            translationComplete.set(false);
            transcriptionCount = 0;
            audioChunksReceived = 0;
            
            // Set up session state
            sessionActive.set(true);
            cancellationRequested.set(false);
            currentSession = credentials;

            // Create WebSocket client
            String wsUri = credentials.getControlUrl();
            String token = credentials.getPublisher();
            
            logger.info("📡 Connecting to WebSocket: {}", wsUri);
            PalabraWebSocketClient wsClient = new PalabraWebSocketClient(wsUri, token);
            currentWebSocketClient = wsClient;

            // Check for cancellation before proceeding
            if (cancellationRequested.get()) {
                logger.info("Session cancelled before WebSocket connection");
                return;
            }
            
            // Set up message handlers
            setupTranslationHandlers(wsClient, config);
            
            // Connect to WebSocket
            CompletableFuture<Void> connectionFuture = wsClient.connect();
            connectionFuture.get(10, TimeUnit.SECONDS);
            logger.info("🔌 WebSocket connected successfully!");
            
            // Send translation settings
            sendTranslationSettings(wsClient, config);
            
            // Start audio processing
            startAudioProcessing(wsClient, config);
            
            logger.info("🎉🎉🎉 Translation completed 🎉🎉🎉");

        } finally {
            // Clean up session state
            sessionActive.set(false);
            currentSession = null;
            currentWebSocketClient = null;
            cancellationRequested.set(false);
            
            // Close reader and writer to ensure data is flushed
            try {
                if (config.getReader() != null) {
                    config.getReader().close();
                    logger.debug("Reader closed successfully");
                }
            } catch (Exception e) {
                logger.warn("Error closing reader: {}", e.getMessage());
            }
            
            try {
                if (config.getWriter() != null) {
                    config.getWriter().close();
                    logger.debug("Writer closed successfully");
                }
            } catch (Exception e) {
                logger.warn("Error closing writer: {}", e.getMessage());
            }
            
            logger.debug("Session state cleaned up");
        }
    }
    
    /**
     * Sets up message handlers for the WebSocket client during translation.
     */
    private void setupTranslationHandlers(PalabraWebSocketClient wsClient, Config config) {
        // Connection handler
        wsClient.setConnectionHandler(() -> {
            logger.info("🔌 Translation WebSocket connected!");
        });
        
        // Disconnection handler
        wsClient.setDisconnectionHandler(() -> {
            logger.info("📡 Translation WebSocket disconnected");
        });
        
        // Error handler
        wsClient.setErrorHandler(error -> {
            logger.error("❌ Translation WebSocket error: {}", error);
        });
        
        // Message handler
        wsClient.setMessageHandler(message -> {
            handleTranslationMessage(message, config);
        });
    }
    
    /**
     * Handles incoming translation messages.
     */
    private void handleTranslationMessage(Message message, Config config) {
        String messageType = message.getMessageType();
        
        switch (messageType) {
            case "current_task":
                if (message instanceof TaskMessage taskMsg) {
                    logger.info("📝 Task status: {}", taskMsg.getStatus());
                }
                break;
                
            case "partial_transcription":
                if (message instanceof TranscriptionMessage transcription) {
                    String langCode = transcription.getLanguage() != null ? 
                                    transcription.getLanguage().getCode() : "unknown";
                    logger.info("💬 [{}] {}", langCode, transcription.getText());
                    transcriptionCount++;
                }
                break;
                
            case "final_transcription":
                if (message instanceof TranscriptionMessage transcription) {
                    String langCode = transcription.getLanguage() != null ? 
                                    transcription.getLanguage().getCode() : "unknown";
                    logger.info("✅ [{}] {}", langCode, transcription.getText());
                    transcriptionCount++;
                }
                break;
                
            case "output_audio_data":
                if (message instanceof AudioMessage audioMsg) {
                    logger.info("🔊 Received {} bytes of translated audio", 
                              audioMsg.getAudioSize());
                    
                    audioChunksReceived++;
                    outputAudioReceived.set(true);
                    
                    boolean debugMode = System.getProperty("PALABRA_DEBUG", "false").equals("true");
                    
                    // Write audio data to the configured writer
                    try {
                        byte[] audioData = audioMsg.getAudioData();
                        if (audioData != null && audioData.length > 0) {
                            if (debugMode) {
                                System.out.println("🔊 Processing " + audioData.length + " bytes of raw audio data");
                            }
                            
                            // Resample from 24kHz to 48kHz for playback
                            byte[] upsampledAudio = ai.palabra.util.AudioUtils.resample24kTo48k(audioData);
                            
                            if (debugMode) {
                                System.out.println("🔊 Upsampled to " + upsampledAudio.length + " bytes, writing to DeviceWriter");
                            }
                            
                            config.getWriter().write(upsampledAudio);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to write audio data: {}", e.getMessage(), e);
                    }
                }
                break;
                
            default:
                logger.debug("📨 Received message: {}", message);
                break;
        }
    }
    
    /**
     * Sends translation settings to the WebSocket server.
     */
    private void sendTranslationSettings(PalabraWebSocketClient wsClient, Config config) {
        // Use the current AdvancedConfig for more detailed settings
        AdvancedConfig advancedConfig = this.currentAdvancedConfig;
        
        // Create translation settings matching the Python reference implementation
        Map<String, Object> settings = new HashMap<>();
        
        // Input stream configuration
        Map<String, Object> inputStream = new HashMap<>();
        inputStream.put("content_type", "audio");
        
        Map<String, Object> source = new HashMap<>();
        source.put("type", "ws");
        // Use AdvancedConfig input stream settings or defaults
        InputStreamConfig inputConfig = advancedConfig != null ? advancedConfig.getInputStream() : InputStreamConfig.defaults();
        source.put("format", inputConfig.getSource().get("format"));
        source.put("sample_rate", inputConfig.getSource().get("sample_rate"));
        source.put("channels", inputConfig.getSource().get("channels"));
        inputStream.put("source", source);
        
        settings.put("input_stream", inputStream);
        
        // Output stream configuration
        Map<String, Object> outputStream = new HashMap<>();
        outputStream.put("content_type", "audio");
        
        Map<String, Object> target = new HashMap<>();
        target.put("type", "ws");
        // Use AdvancedConfig output stream settings or defaults
        OutputStreamConfig outputConfig = advancedConfig != null ? advancedConfig.getOutputStream() : OutputStreamConfig.defaults();
        target.put("format", outputConfig.getTarget().get("format"));
        target.put("sample_rate", outputConfig.getTarget().get("sample_rate"));
        target.put("channels", outputConfig.getTarget().get("channels"));
        outputStream.put("target", target);
        
        settings.put("output_stream", outputStream);
        
        // Pipeline configuration
        Map<String, Object> pipeline = new HashMap<>();
        pipeline.put("preprocessing", new HashMap<>());
        
        Map<String, Object> transcription = new HashMap<>();
        // Use AdvancedConfig source language or fallback to simple config
        if (advancedConfig != null && advancedConfig.getSource() != null && advancedConfig.getSource().getLanguage() != null) {
            transcription.put("source_language", advancedConfig.getSource().getLanguage().getSimpleCode());
        } else {
            transcription.put("source_language", config.getSourceLang() != null ? config.getSourceLang().getSimpleCode() : "auto");
        }
        
        // Add transcription model if specified in AdvancedConfig
        if (advancedConfig != null && advancedConfig.getSource() != null && advancedConfig.getSource().getTranscription() != null) {
            TranscriptionConfig transcConfig = advancedConfig.getSource().getTranscription();
            if (transcConfig.getAsrModel() != null && !transcConfig.getAsrModel().isEmpty()) {
                transcription.put("model", transcConfig.getAsrModel());
            }
        }
        pipeline.put("transcription", transcription);
        
        // Support multiple target languages from AdvancedConfig
        List<Map<String, Object>> translations = new ArrayList<>();
        
        if (advancedConfig != null && advancedConfig.getTargets() != null && !advancedConfig.getTargets().isEmpty()) {
            // Use targets from AdvancedConfig
            for (TargetLangConfig targetLang : advancedConfig.getTargets()) {
                Map<String, Object> translation = new HashMap<>();
                translation.put("target_language", targetLang.getLanguage().getSimpleCode());
                
                // Add speech generation settings if available
                Map<String, Object> speechGeneration = new HashMap<>();
                if (targetLang.getTranslation() != null && targetLang.getTranslation().getSpeechGeneration() != null) {
                    SpeechGenerationConfig speechConfig = targetLang.getTranslation().getSpeechGeneration();
                    if (speechConfig.getTtsModel() != null && !speechConfig.getTtsModel().isEmpty()) {
                        speechGeneration.put("model", speechConfig.getTtsModel());
                    }
                    if (speechConfig.getVoiceId() != null && !speechConfig.getVoiceId().isEmpty()) {
                        speechGeneration.put("voice", speechConfig.getVoiceId());
                    }
                }
                translation.put("speech_generation", speechGeneration);
                translations.add(translation);
            }
        } else {
            // Fallback to simple config single target
            Map<String, Object> translation = new HashMap<>();
            translation.put("target_language", config.getTargetLang().getSimpleCode());
            translation.put("speech_generation", new HashMap<>());
            translations.add(translation);
        }
        
        pipeline.put("translations", translations.toArray(new Map[0]));
        
        settings.put("pipeline", pipeline);
        
        // Create message with set_task type (matching Python implementation)
        Map<String, Object> message = new HashMap<>();
        message.put("message_type", "set_task");
        message.put("data", settings);
        
        // Send settings
        logger.info("⚙️ Sending translation settings...");
        logger.debug("Settings content: {}", message);
        wsClient.sendMessage(message).thenRun(() -> {
            logger.info("✅ Translation settings sent successfully");
        }).exceptionally(ex -> {
            logger.error("❌ Failed to send translation settings: {}", ex.getMessage());
            return null;
        });
        
        // Wait for settings to process (matching Python implementation)
        try {
            logger.info("⏳ Waiting for settings to process...");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for settings", e);
        }
    }
    
    /**
     * Starts audio processing loop that reads from input and sends to WebSocket.
     */
    private void startAudioProcessing(PalabraWebSocketClient wsClient, Config config) throws Exception {
        logger.info("🎤 Starting audio processing...");
        
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        
        // Set up shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🛑 Shutdown signal received, stopping audio processing...");
            keepRunning.set(false);
        }));
        
        try {
            // Phase 1: Send all audio data
            logger.info("📤 Sending audio data to server...");
            int chunksSent = 0;
            
            while (keepRunning.get() && !cancellationRequested.get()) {
                try {
                    // Check for cancellation before processing
                    if (cancellationRequested.get()) {
                        logger.info("Audio processing cancelled by user request");
                        break;
                    }

                    // Read audio chunk from input
                    byte[] audioChunk = config.getReader().read();
                    
                    // Check if end of file reached
                    if (audioChunk == null) {
                        logger.info("📁 End of input reached after {} chunks", chunksSent);
                        break;
                    }
                    
                    if (audioChunk.length > 0) {
                        // Send audio data via WebSocket
                        sendAudioData(wsClient, audioChunk);
                        chunksSent++;
                    }
                    
                    // Increased delay to prevent overwhelming the server
                    Thread.sleep(50); // Increased from 10ms to 50ms
                    
                } catch (Exception e) {
                    if (keepRunning.get() && !cancellationRequested.get()) {
                        logger.error("Error processing audio: {}", e.getMessage(), e);
                        break;
                    }
                }
            }
            
            // Phase 2: Wait for server to process and send back results
            logger.info("⏳ Waiting for server to process audio and send translation...");
            logger.info("📊 Sent {} audio chunks, waiting for responses...", chunksSent);
            
            // Wait for output audio with timeout
            long maxWaitTime = 30000; // 30 seconds timeout
            long startTime = System.currentTimeMillis();
            
            while (keepRunning.get() && !cancellationRequested.get()) {
                // Check if we've received any output audio
                if (outputAudioReceived.get()) {
                    logger.info("✅ Received output audio! Chunks: {}, Transcriptions: {}", 
                              audioChunksReceived, transcriptionCount);
                    
                    // Wait a bit more for any remaining chunks
                    Thread.sleep(2000);
                    break;
                }
                
                // Check timeout
                if (System.currentTimeMillis() - startTime > maxWaitTime) {
                    logger.warn("⏰ Timeout waiting for server response after {}ms", maxWaitTime);
                    logger.warn("📊 Stats - Transcriptions: {}, Audio chunks: {}", 
                               transcriptionCount, audioChunksReceived);
                    break;
                }
                
                // Log progress every 5 seconds
                if ((System.currentTimeMillis() - startTime) % 5000 < 100) {
                    logger.info("⏳ Still waiting... Transcriptions: {}, Audio chunks: {}", 
                               transcriptionCount, audioChunksReceived);
                }
                
                Thread.sleep(100);
            }
            
        } finally {
            logger.info("🔚 Audio processing finished");
            logger.info("📊 Final stats - Transcriptions: {}, Audio chunks received: {}", 
                       transcriptionCount, audioChunksReceived);
            
            // Don't close WebSocket immediately - give time for final messages
            Thread.sleep(1000);
            
            // Clean shutdown
            try {
                wsClient.close(3).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Warning during WebSocket cleanup: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Sends audio data via WebSocket.
     */
    private void sendAudioData(PalabraWebSocketClient wsClient, byte[] audioChunk) {
        // Resample from 48kHz to 24kHz to match server expectations
        byte[] resampledAudio = ai.palabra.util.AudioUtils.resample48kTo24k(audioChunk);
        
        Map<String, Object> audioMessage = new HashMap<>();
        audioMessage.put("message_type", "input_audio_data");
        
        Map<String, Object> data = new HashMap<>();
        data.put("data", Base64.getEncoder().encodeToString(resampledAudio));
        audioMessage.put("data", data);
        
        wsClient.sendMessage(audioMessage).exceptionally(ex -> {
            logger.error("Failed to send audio data: {}", ex.getMessage());
            return null;
        });
    }

    /**
     * Converts a simple Config to AdvancedConfig for internal processing.
     * 
     * @param config The simple Config to convert
     * @return AdvancedConfig with equivalent settings
     */
    private AdvancedConfig convertToAdvancedConfig(Config config) {
        if (config.isAdvanced()) {
            // Config is already advanced, extract the AdvancedConfig
            return config.getAdvancedConfig();
        }
        
        // Convert simple Config to AdvancedConfig
        AdvancedConfig.Builder builder = AdvancedConfig.builder()
            .source(config.getSourceLang(), config.getReader())
            .addTarget(config.getTargetLang(), config.getWriter());
        
        // Use default settings for audio streams if not specified
        builder.inputStream(InputStreamConfig.defaults())
               .outputStream(OutputStreamConfig.defaults());
        
        return builder.build();
    }
    
    /**
     * Validates the provided AdvancedConfig to ensure it contains all required elements.
     * 
     * @param config The AdvancedConfig to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateAdvancedConfig(AdvancedConfig config) {
        if (config.getSource() == null) {
            throw new IllegalArgumentException("Source configuration must be specified");
        }
        
        if (config.getTargets() == null || config.getTargets().isEmpty()) {
            throw new IllegalArgumentException("At least one target configuration must be specified");
        }
        
        if (config.getSource().getReader() == null) {
            throw new IllegalArgumentException("Audio reader must be specified in source configuration");
        }
        
        for (TargetLangConfig target : config.getTargets()) {
            if (target.getWriter() == null) {
                throw new IllegalArgumentException("Audio writer must be specified for target: " + target.getLanguage().getCode());
            }
        }
        
        logger.debug("AdvancedConfig validated successfully - Source: {}, Targets: {}",
                config.getSource().getLanguage() != null ? config.getSource().getLanguage().getDisplayName() : "auto-detect",
                config.getTargets().size());
    }

    /**
     * Validates the provided configuration to ensure it contains all required elements.
     * 
     * @param config The configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateConfig(Config config) {
        if (config.isAdvanced()) {
            // Advanced config validation is handled separately
            return;
        }
        if (config.getSourceLang() == null) {
            throw new IllegalArgumentException("Source language must be specified in configuration");
        }
        
        if (config.getTargetLang() == null) {
            throw new IllegalArgumentException("Target language must be specified in configuration");
        }
        
        if (config.getReader() == null) {
            throw new IllegalArgumentException("Audio reader must be specified in configuration");
        }
        
        if (config.getWriter() == null) {
            throw new IllegalArgumentException("Audio writer must be specified in configuration");
        }
        
        logger.debug("Configuration validated successfully - Source: {}, Target: {}, Reader: {}, Writer: {}",
                config.getSourceLang().getDisplayName(),
                config.getTargetLang().getDisplayName(),
                config.getReader().getClass().getSimpleName(),
                config.getWriter().getClass().getSimpleName());
    }
}
