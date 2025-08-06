package ai.palabra.base;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Generic message for unknown message types.
 */
public class GenericMessage extends Message {
    /**
     * Creates a new generic message from WebSocket data.
     * 
     * @param messageType the type of message
     * @param data the JSON data
     */
    public GenericMessage(String messageType, JsonNode data) {
        super(messageType, data);
    }
    
    @Override
    public String toString() {
        return String.format("GenericMessage{type='%s'}", messageType);
    }
}
