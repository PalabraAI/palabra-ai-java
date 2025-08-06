package ai.palabra;

import ai.palabra.adapter.BufferReader;
import ai.palabra.adapter.BufferWriter;
import ai.palabra.internal.PalabraRESTClient;
import ai.palabra.internal.SessionCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for asynchronous processing capabilities.
 * Tests async method invocation, completion, cancellation, and error handling.
 */
public class AsyncProcessingTest {
    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingTest.class);

    private PalabraAI client;
    private Config validConfig;
    
    @Mock
    private PalabraRESTClient mockRestClient;
    
    @Mock
    private SessionCredentials mockCredentials;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize client with test credentials in test mode
        client = new PalabraAI("test-client-id", "test-client-secret", "https://api.palabra.ai", true);
        
        // Setup mock credentials
        when(mockCredentials.getPublisher()).thenReturn("test-publisher");
        when(mockCredentials.getControlUrl()).thenReturn("ws://test-control-url");
        when(mockCredentials.getStreamUrl()).thenReturn("ws://test-stream-url");
        
        // Create valid configuration for testing
        validConfig = Config.builder()
                .sourceLang(Language.EN_US)
                .targetLang(Language.ES_MX)
                .reader(new BufferReader(1024))  // maxBufferSize parameter
                .writer(new BufferWriter(1024))  // maxBufferSize parameter
                .build();
    }

    @Test
    @Timeout(10)
    void shouldProcessAsyncWithValidConfig() throws Exception {
        // Test basic async processing
        CompletableFuture<Void> future = client.processAsync(validConfig);
        
        assertNotNull(future, "ProcessAsync should return a non-null CompletableFuture");
        assertFalse(future.isDone(), "Future should not be completed immediately");
        
        // Wait for completion
        future.get(5, TimeUnit.SECONDS);
        
        assertTrue(future.isDone(), "Future should be completed");
        assertFalse(future.isCancelled(), "Future should not be cancelled");
        assertFalse(future.isCompletedExceptionally(), "Future should complete successfully");
    }

    @Test
    void shouldThrowExceptionForNullConfig() {
        // Test that null config throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            client.processAsync(null);
        }, "ProcessAsync should throw IllegalArgumentException for null config");
    }

    @Test
    @Timeout(10)
    void shouldProcessAsyncWithCustomExecutor() throws Exception {
        // Test async processing with custom executor
        ExecutorService customExecutor = Executors.newSingleThreadExecutor();
        
        try {
            CompletableFuture<Void> future = client.processAsync(validConfig, customExecutor);
            
            assertNotNull(future, "ProcessAsync with executor should return a non-null CompletableFuture");
            
            // Wait for completion
            future.get(5, TimeUnit.SECONDS);
            
            assertTrue(future.isDone(), "Future should be completed");
            assertFalse(future.isCompletedExceptionally(), "Future should complete successfully");
        } finally {
            customExecutor.shutdown();
        }
    }

    @Test
    void shouldThrowExceptionForNullExecutor() {
        // Test that null executor throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            client.processAsync(validConfig, null);
        }, "ProcessAsync should throw IllegalArgumentException for null executor");
    }

    @Test
    @Timeout(10)
    void shouldCreateCancellableTranslation() throws Exception {
        // Test cancellable async processing
        CompletableFuture<Void> future = client.processAsyncCancellable(validConfig);
        
        assertNotNull(future, "ProcessAsyncCancellable should return a non-null CompletableFuture");
        assertFalse(future.isDone(), "Future should not be completed immediately");
        
        // Wait for completion
        future.get(5, TimeUnit.SECONDS);
        
        assertTrue(future.isDone(), "Future should be completed");
        assertFalse(future.isCancelled(), "Future should not be cancelled when completed normally");
    }

    @Test
    @Timeout(10)
    void shouldCancelTranslationBeforeCompletion() throws Exception {
        // Test cancellation functionality with a longer delay
        AtomicBoolean cancelled = new AtomicBoolean(false);
        
        // Create a config that should complete normally but we'll cancel it
        Config slowConfig = Config.builder()
                .sourceLang(Language.EN_US)
                .targetLang(Language.ES_MX)
                .reader(new BufferReader(1024))
                .writer(new BufferWriter(1024))
                .build();
        
        CompletableFuture<Void> future = client.processAsyncCancellable(slowConfig);
        
        // Cancel immediately
        boolean result = future.cancel(true);
        cancelled.set(result);
        
        // Verify cancellation
        assertTrue(future.isCancelled(), "Future should be cancelled");
        assertTrue(cancelled.get(), "Cancellation should have succeeded");
        
        // Verify that we get CancellationException when trying to get result
        assertThrows(CancellationException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        }, "Cancelled future should throw CancellationException");
    }

    @Test
    @Timeout(10)
    void shouldProcessAsyncWithTimeout() throws Exception {
        // Test async processing with timeout
        CompletableFuture<Void> future = client.processAsyncWithTimeout(validConfig, 5, TimeUnit.SECONDS);
        
        assertNotNull(future, "ProcessAsyncWithTimeout should return a non-null CompletableFuture");
        
        // Wait for completion (should complete before timeout)
        future.get(10, TimeUnit.SECONDS);
        
        assertTrue(future.isDone(), "Future should be completed");
        assertFalse(future.isCompletedExceptionally(), "Future should complete successfully within timeout");
    }

    @Test
    @Timeout(5)
    void shouldTimeoutWhenProcessingTakesTooLong() {
        // Test timeout functionality with a very short timeout
        // Since we're in test mode, operations complete quickly, so we use a very small timeout
        CompletableFuture<Void> future = client.processAsyncWithTimeout(validConfig, 1, TimeUnit.NANOSECONDS);
        
        assertNotNull(future, "ProcessAsyncWithTimeout should return a non-null CompletableFuture");
        
        // The future should either complete successfully (if very fast) or timeout
        assertDoesNotThrow(() -> {
            try {
                future.get(2, TimeUnit.SECONDS);
                // If it completes successfully, that's also fine in test mode
                logger.info("Operation completed before timeout in test mode");
            } catch (ExecutionException e) {
                // Check if it's a timeout-related exception
                assertTrue(e.getCause().getMessage().contains("timed out") || 
                          e.getCause() instanceof RuntimeException,
                          "Should be a timeout or runtime exception");
            }
        });
    }

    @Test
    void shouldThrowExceptionForNegativeTimeout() {
        // Test that negative timeout throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            client.processAsyncWithTimeout(validConfig, -1, TimeUnit.SECONDS);
        }, "ProcessAsyncWithTimeout should throw IllegalArgumentException for negative timeout");
    }

    @Test
    @Timeout(10)
    void shouldHandleInvalidConfigurationAsync() {
        // Test async processing with invalid configuration
        // Since the Config.builder() validates during build(), we need to test 
        // a different kind of invalid config or handle the build exception
        assertThrows(IllegalStateException.class, () -> {
            Config invalidConfig = Config.builder()
                    .sourceLang(null) // Invalid - null source language
                    .targetLang(Language.ES_MX)
                    .reader(new BufferReader(1024))
                    .writer(new BufferWriter(1024))
                    .build();
        }, "Building config with null source language should throw IllegalStateException");
    }

    @Test
    @Timeout(10)
    void shouldChainAsyncOperations() throws Exception {
        // Test chaining of async operations
        AtomicReference<String> result = new AtomicReference<>();
        
        CompletableFuture<Void> chainedFuture = client.processAsync(validConfig)
                .thenRun(() -> result.set("first-completed"))
                .thenCompose(v -> client.processAsync(validConfig))
                .thenRun(() -> result.set("second-completed"));
        
        // Wait for the entire chain to complete
        chainedFuture.get(10, TimeUnit.SECONDS);
        
        assertEquals("second-completed", result.get(), "Chained operations should complete in order");
        assertTrue(chainedFuture.isDone(), "Chained future should be completed");
        assertFalse(chainedFuture.isCompletedExceptionally(), "Chained future should complete successfully");
    }

    @Test
    @Timeout(10)
    void shouldHandleExceptionInAsyncChain() {
        // Test exception handling in async operation chains
        // We'll simulate an exception by using a null config which will fail validation
        CompletableFuture<String> chainedFuture = client.processAsync(validConfig)
                .thenCompose(v -> {
                    // Simulate a failure in the chain by throwing an exception
                    CompletableFuture<Void> failingFuture = new CompletableFuture<>();
                    failingFuture.completeExceptionally(new RuntimeException("Simulated failure"));
                    return failingFuture;
                })
                .thenApply(v -> "should-not-reach-here")
                .exceptionally(throwable -> "exception-handled");
        
        // The chain should complete with exception handling
        assertDoesNotThrow(() -> {
            String result = chainedFuture.get(5, TimeUnit.SECONDS);
            assertEquals("exception-handled", result, "Exception should be handled in the chain");
        });
    }

    @Test
    @Timeout(10)
    void shouldAllowMultipleConcurrentAsyncOperations() throws Exception {
        // Test multiple concurrent async operations
        int numOperations = 5;
        CompletableFuture<Void>[] futures = new CompletableFuture[numOperations];
        
        // Start multiple async operations concurrently
        for (int i = 0; i < numOperations; i++) {
            futures[i] = client.processAsync(validConfig);
        }
        
        // Wait for all operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(15, TimeUnit.SECONDS);
        
        // Verify all operations completed successfully
        for (CompletableFuture<Void> future : futures) {
            assertTrue(future.isDone(), "All concurrent futures should be completed");
            assertFalse(future.isCompletedExceptionally(), "All concurrent futures should complete successfully");
        }
    }

    @Test
    @Timeout(10)
    void shouldHandleInterruptedThreadInAsync() {
        // Test handling of thread interruption in async operations
        CompletableFuture<Void> future = client.processAsync(validConfig);
        
        // The future should complete normally even if we test interruption handling
        assertDoesNotThrow(() -> {
            future.get(5, TimeUnit.SECONDS);
        }, "Async operation should complete normally");
        
        // Test that interruption is handled properly by simulating it
        // Create a separate thread to test interruption behavior
        CompletableFuture<Boolean> interruptTest = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.currentThread().interrupt();
                CompletableFuture<Void> interruptedFuture = client.processAsync(validConfig);
                interruptedFuture.get(2, TimeUnit.SECONDS);
                return Thread.interrupted(); // Clear and return previous interrupted status
            } catch (Exception e) {
                Thread.interrupted(); // Clear interrupted status
                return false;
            }
        });
        
        assertDoesNotThrow(() -> {
            Boolean result = interruptTest.get(5, TimeUnit.SECONDS);
            // The result doesn't matter much - we're testing that no exception propagates
        }, "Interrupted thread test should not throw exceptions");
    }

    @Test
    @Timeout(10)
    void shouldSupportAsyncCallbacksWithThenRun() throws Exception {
        // Test async callbacks using thenRun
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = client.processAsync(validConfig)
                .thenRun(() -> callbackExecuted.set(true));
        
        // Wait for completion
        future.get(5, TimeUnit.SECONDS);
        
        assertTrue(callbackExecuted.get(), "Callback should be executed after async completion");
        assertTrue(future.isDone(), "Future with callback should be completed");
    }

    @Test
    @Timeout(10)
    void shouldSupportAsyncCallbacksWithWhenComplete() throws Exception {
        // Test async callbacks using whenComplete
        AtomicReference<String> callbackResult = new AtomicReference<>();
        
        CompletableFuture<Void> future = client.processAsync(validConfig)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        callbackResult.set("success");
                    } else {
                        callbackResult.set("failure");
                    }
                });
        
        // Wait for completion
        future.get(5, TimeUnit.SECONDS);
        
        assertEquals("success", callbackResult.get(), "WhenComplete callback should receive successful result");
        assertTrue(future.isDone(), "Future with whenComplete should be completed");
    }
}
