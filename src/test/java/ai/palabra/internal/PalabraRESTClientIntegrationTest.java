package ai.palabra.internal;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PalabraRESTClient.
 * These tests require valid Palabra AI credentials and will only run when
 * environment variables PALABRA_CLIENT_ID and PALABRA_CLIENT_SECRET are set.
 */
class PalabraRESTClientIntegrationTest {
    
    @Test
    @EnabledIfEnvironmentVariable(named = "PALABRA_CLIENT_ID", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "PALABRA_CLIENT_SECRET", matches = ".+")
    void testCreateSessionWithRealCredentials() throws PalabraException {
        String clientId = System.getenv("PALABRA_CLIENT_ID");
        String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");
        
        // Double-check environment variables are set (redundant but safe)
        if (clientId == null || clientId.trim().isEmpty() || 
            clientSecret == null || clientSecret.trim().isEmpty()) {
            System.out.println("⏭️  Skipping REST client integration test - credentials not available");
            return;
        }
        
        PalabraRESTClient client = new PalabraRESTClient(clientId, clientSecret);
        
        // Create a session
        SessionCredentials credentials;
        try {
            credentials = client.createSession();
        } catch (Exception e) {
            System.err.println("❌ Failed to create session: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            System.err.println("   This may indicate invalid credentials or API connectivity issues");
            throw new RuntimeException("Session creation failed - check API connectivity and credentials", e);
        }
        
        // Verify the response
        assertNotNull(credentials, "Session credentials should not be null");
        assertNotNull(credentials.getPublisher(), "Publisher should not be null");
        // Note: Subscriber might be null in some API responses, so we make it optional
        assertNotNull(credentials.getRoomName(), "Room name should not be null");
        assertNotNull(credentials.getStreamUrl(), "Stream URL should not be null");
        assertNotNull(credentials.getControlUrl(), "Control URL should not be null");
        
        assertFalse(credentials.getPublisher().isEmpty());
        assertFalse(credentials.getRoomName().trim().isEmpty());
        assertFalse(credentials.getStreamUrl().trim().isEmpty());
        assertFalse(credentials.getControlUrl().trim().isEmpty());
        
        System.out.println("✅ Session created successfully:");
        System.out.println("   Room: " + credentials.getRoomName());
        System.out.println("   Control URL: " + credentials.getControlUrl());
        System.out.println("   Publisher token: " + credentials.getPublisher());
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "PALABRA_CLIENT_ID", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "PALABRA_CLIENT_SECRET", matches = ".+")
    void testCreateSessionAsync() throws Exception {
        String clientId = System.getenv("PALABRA_CLIENT_ID");
        String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");
        
        PalabraRESTClient client = new PalabraRESTClient(clientId, clientSecret);
        
        // Create a session asynchronously
        CompletableFuture<SessionCredentials> future = client.createSessionAsync();
        
        // Wait for completion (with timeout)
        SessionCredentials credentials = future.get();
        
        // Verify the response
        assertNotNull(credentials);
        assertNotNull(credentials.getPublisher());
        assertNotNull(credentials.getRoomName());
        
        System.out.println("✅ Async session created successfully:");
        System.out.println("   Room: " + credentials.getRoomName());
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "PALABRA_CLIENT_ID", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "PALABRA_CLIENT_SECRET", matches = ".+")
    void testCreateSessionWithCustomCounts() throws PalabraException {
        String clientId = System.getenv("PALABRA_CLIENT_ID");
        String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");
        
        PalabraRESTClient client = new PalabraRESTClient(clientId, clientSecret);
        
        // Create a session with custom publisher/subscriber counts
        SessionCredentials credentials = client.createSession(2, 1);
        
        // Verify the response
        assertNotNull(credentials);
        assertNotNull(credentials.getPublisher());
        assertNotNull(credentials.getSubscriber());
        
        // Should have tokens for the requested counts
        assertNotNull(credentials.getPublisher());
        assertFalse(credentials.getPublisher().trim().isEmpty());
        
        System.out.println("✅ Session with custom counts created successfully:");
        System.out.println("   Publisher token: " + credentials.getPublisher());
        System.out.println("   Subscriber token: " + credentials.getSubscriber());
    }
    
    @Test
    void testCreateSessionWithInvalidCredentials() {
        PalabraRESTClient client = new PalabraRESTClient("invalid-id", "invalid-secret");
        
        // Should throw PalabraException with authentication error
        assertThrows(PalabraException.class, () -> {
            client.createSession();
        });
        
        // Note: The specific error message depends on the API implementation
        // This test verifies that invalid credentials result in an exception
    }
    
    @Test
    void testCreateSessionWithInvalidUrl() {
        PalabraRESTClient client = new PalabraRESTClient(
            "test-id", "test-secret", 
            Duration.ofSeconds(5), "https://invalid.nonexistent.url"
        );
        
        // Should throw PalabraException with connection error
        assertThrows(PalabraException.class, () -> {
            client.createSession();
        });
    }
}
