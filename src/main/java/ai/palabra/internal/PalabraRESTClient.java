package ai.palabra.internal;

import ai.palabra.PalabraException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST client for Palabra AI session management.
 * Handles authentication and translation session creation via HTTP API.
 */
public class PalabraRESTClient {
    private static final Logger logger = LoggerFactory.getLogger(PalabraRESTClient.class);
    
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new REST client for Palabra AI.
     * 
     * @param clientId Client ID for authentication
     * @param clientSecret Client secret for authentication
     */
    public PalabraRESTClient(String clientId, String clientSecret) {
        this(clientId, clientSecret, Duration.ofSeconds(5), "https://api.palabra.ai");
    }
    
    /**
     * Creates a new REST client with custom configuration.
     * 
     * @param clientId Client ID for authentication
     * @param clientSecret Client secret for authentication
     * @param timeout Request timeout
     * @param baseUrl Base URL for the API
     */
    public PalabraRESTClient(String clientId, String clientSecret, Duration timeout, String baseUrl) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("Client secret cannot be null or empty");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(5);
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(this.timeout)
            .build();
            
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Creates a new translation session synchronously.
     * 
     * @return Session credentials for the new session
     * @throws PalabraException if session creation fails
     */
    public SessionCredentials createSession() throws PalabraException {
        return createSession(1, 0);
    }
    
    /**
     * Creates a new translation session with specified publisher/subscriber counts.
     * 
     * @param publisherCount Number of publishers (default: 1)
     * @param subscriberCount Number of subscribers (default: 0)
     * @return Session credentials for the new session
     * @throws PalabraException if session creation fails
     */
    public SessionCredentials createSession(int publisherCount, int subscriberCount) throws PalabraException {
        try {
            return createSessionAsync(publisherCount, subscriberCount).join();
        } catch (Exception e) {
            if (e.getCause() instanceof PalabraException) {
                throw (PalabraException) e.getCause();
            }
            throw new PalabraException("Failed to create session", e);
        }
    }
    
    /**
     * Creates a new translation session asynchronously.
     * 
     * @return CompletableFuture with session credentials
     */
    public CompletableFuture<SessionCredentials> createSessionAsync() {
        return createSessionAsync(1, 0);
    }
    
    /**
     * Creates a new translation session asynchronously with specified counts.
     * 
     * @param publisherCount Number of publishers
     * @param subscriberCount Number of subscribers  
     * @return CompletableFuture with session credentials
     */
    public CompletableFuture<SessionCredentials> createSessionAsync(int publisherCount, int subscriberCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Creating session with subscriber_count={}, publisher_can_subscribe=true", 
                           subscriberCount);
                
                // Prepare request payload - match Python implementation
                Map<String, Object> requestData = Map.of(
                    "data", Map.of(
                        "subscriber_count", subscriberCount,
                        "publisher_can_subscribe", true
                    )
                );
                
                String requestBody = objectMapper.writeValueAsString(requestData);
                
                // Build HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/session-storage/session"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("ClientId", clientId)
                    .header("ClientSecret", clientSecret)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                // Send request
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // Parse response
                Map<String, Object> responseBody = objectMapper.readValue(
                    response.body(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                // Check if response indicates success
                Object okField = responseBody.get("ok");
                if (okField == null || !Boolean.TRUE.equals(okField)) {
                    // Handle error response
                    handleErrorResponse(response.statusCode(), responseBody);
                }
                
                Object dataField = responseBody.get("data");
                if (dataField == null) {
                    throw new PalabraException("Missing data field in response");
                }
                
                // Convert data to SessionCredentials
                SessionCredentials credentials = objectMapper.convertValue(dataField, SessionCredentials.class);
                
                logger.debug("Session created successfully: room={}, control_url={}", 
                           credentials.getRoomName(), credentials.getControlUrl());
                
                return credentials;
                
            } catch (IOException | InterruptedException e) {
                logger.error("Error creating session", e);
                throw new RuntimeException(new PalabraException("Failed to create session", e));
            } catch (Exception e) {
                logger.error("Unexpected error creating session", e);
                throw new RuntimeException(new PalabraException("Unexpected error creating session", e));
            }
        });
    }
    
    /**
     * Handles error responses from the Palabra AI API.
     * Parses the error structure and throws a detailed PalabraException.
     * 
     * @param statusCode HTTP status code
     * @param responseBody Parsed response body
     * @throws PalabraException with detailed error information
     */
    private void handleErrorResponse(int statusCode, Map<String, Object> responseBody) throws PalabraException {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("API request failed with HTTP ").append(statusCode);
        
        // Parse errors array if present
        Object errorsField = responseBody.get("errors");
        if (errorsField instanceof List<?> errorsList) {
            if (!errorsList.isEmpty()) {
                errorMessage.append(": ");
                
                for (int i = 0; i < errorsList.size(); i++) {
                    if (i > 0) {
                        errorMessage.append("; ");
                    }
                    
                    if (errorsList.get(i) instanceof Map<?, ?> errorMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> error = (Map<String, Object>) errorMap;
                        
                        // Extract error details
                        Object title = error.get("title");
                        Object detail = error.get("detail");
                        Object errorCode = error.get("error_code");
                        Object type = error.get("type");
                        Object instance = error.get("instance");
                        Object status = error.get("status");
                        
                        // Build detailed error message
                        if (title != null) {
                            errorMessage.append(title);
                        }
                        if (detail != null) {
                            if (title != null) {
                                errorMessage.append(" - ");
                            }
                            errorMessage.append(detail);
                        }
                        
                        // Log detailed error information
                        logger.error("API Error Details: title='{}', detail='{}', error_code={}, " +
                                   "status={}, type='{}', instance='{}'", 
                                   title, detail, errorCode, status, type, instance);
                    }
                }
            } else {
                errorMessage.append(" (no error details provided)");
                logger.error("API returned ok=false but no errors array or empty errors array");
            }
        } else {
            errorMessage.append(" (invalid error format)");
            logger.error("API returned ok=false but errors field is not a valid array: {}", errorsField);
        }
        
        // Log the full response body for debugging
        logger.debug("Full error response: {}", responseBody);
        
        throw new PalabraException(errorMessage.toString());
    }
    
    /**
     * Gets the client ID.
     * 
     * @return Client ID
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Gets the base URL.
     * 
     * @return Base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Gets the request timeout.
     * 
     * @return Timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }
}
