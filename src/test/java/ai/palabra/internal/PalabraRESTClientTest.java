package ai.palabra.internal;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PalabraRESTClient.
 * Note: These are unit tests that test the client construction and validation.
 * Integration tests with actual API calls would require valid credentials.
 */
class PalabraRESTClientTest {
    
    private static final String VALID_CLIENT_ID = "test-client-id";
    private static final String VALID_CLIENT_SECRET = "test-client-secret";
    
    @Test
    void testDefaultConstructor() {
        PalabraRESTClient client = new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET);
        
        assertEquals(VALID_CLIENT_ID, client.getClientId());
        assertEquals("https://api.palabra.ai", client.getBaseUrl());
        assertEquals(Duration.ofSeconds(5), client.getTimeout());
    }
    
    @Test
    void testParameterizedConstructor() {
        Duration timeout = Duration.ofSeconds(10);
        String baseUrl = "https://custom.api.com";
        
        PalabraRESTClient client = new PalabraRESTClient(
            VALID_CLIENT_ID, VALID_CLIENT_SECRET, timeout, baseUrl
        );
        
        assertEquals(VALID_CLIENT_ID, client.getClientId());
        assertEquals(baseUrl, client.getBaseUrl());
        assertEquals(timeout, client.getTimeout());
    }
    
    @Test
    void testConstructorWithTrailingSlashInUrl() {
        String baseUrlWithSlash = "https://api.test.com/";
        
        PalabraRESTClient client = new PalabraRESTClient(
            VALID_CLIENT_ID, VALID_CLIENT_SECRET, Duration.ofSeconds(5), baseUrlWithSlash
        );
        
        assertEquals("https://api.test.com", client.getBaseUrl());
    }
    
    @Test
    void testConstructorValidation() {
        // Test null client ID
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient(null, VALID_CLIENT_SECRET));
        
        // Test empty client ID
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient("", VALID_CLIENT_SECRET));
        
        // Test whitespace client ID
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient("   ", VALID_CLIENT_SECRET));
        
        // Test null client secret
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient(VALID_CLIENT_ID, null));
        
        // Test empty client secret
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient(VALID_CLIENT_ID, ""));
        
        // Test whitespace client secret
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient(VALID_CLIENT_ID, "   "));
        
        // Test null base URL
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET, Duration.ofSeconds(5), null));
        
        // Test empty base URL
        assertThrows(IllegalArgumentException.class, () ->
            new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET, Duration.ofSeconds(5), ""));
    }
    
    @Test
    void testNullTimeoutHandling() {
        PalabraRESTClient client = new PalabraRESTClient(
            VALID_CLIENT_ID, VALID_CLIENT_SECRET, null, "https://api.test.com"
        );
        
        assertEquals(Duration.ofSeconds(5), client.getTimeout());
    }
    
    @Test
    void testCreateSessionDefaultParameters() {
        PalabraRESTClient client = new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET);
        
        // This will fail with network error since we don't have a real API,
        // but it tests that the method exists and accepts the right parameters
        assertThrows(Exception.class, () -> client.createSession());
    }
    
    @Test
    void testCreateSessionWithParameters() {
        PalabraRESTClient client = new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET);
        
        // This will fail with network error since we don't have a real API,
        // but it tests that the method exists and accepts the right parameters
        assertThrows(Exception.class, () -> client.createSession(2, 1));
    }
    
    @Test
    void testCreateSessionAsync() {
        PalabraRESTClient client = new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET);
        
        // Test that async method returns a CompletableFuture
        var future = client.createSessionAsync();
        assertNotNull(future);
        
        // The future will complete exceptionally due to network error, but that's expected
        assertTrue(future.isCompletedExceptionally() || !future.isDone());
    }
    
    @Test
    void testCreateSessionAsyncWithParameters() {
        PalabraRESTClient client = new PalabraRESTClient(VALID_CLIENT_ID, VALID_CLIENT_SECRET);
        
        // Test that async method returns a CompletableFuture
        var future = client.createSessionAsync(1, 0);
        assertNotNull(future);
        
        // The future will complete exceptionally due to network error, but that's expected
        assertTrue(future.isCompletedExceptionally() || !future.isDone());
    }
}
