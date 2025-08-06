package ai.palabra.base;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Message indicating an error occurred.
 */
public class ErrorMessage extends Message {
    private final String error;
    private final String details;
    
    /**
     * Creates a new error message from WebSocket data.
     * 
     * @param messageType the type of message (typically "error")
     * @param data the JSON data containing error details
     */
    public ErrorMessage(String messageType, JsonNode data) {
        super(messageType, data);
        
        this.error = data.path("error").asText();
        this.details = data.path("details").asText();
    }
    
    /**
     * Gets the error message.
     * 
     * @return The error message
     */
    public String getError() {
        return error;
    }
    
    /**
     * Gets the error details.
     * 
     * @return The error details
     */
    public String getDetails() {
        return details;
    }
    
    @Override
    public String toString() {
        return String.format("ErrorMessage{error='%s', details='%s'}", error, details);
    }
}
