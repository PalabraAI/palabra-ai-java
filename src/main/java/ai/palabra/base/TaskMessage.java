package ai.palabra.base;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Message indicating current task status.
 */
public class TaskMessage extends Message {
    /**
     * Creates a new task message from WebSocket data.
     * 
     * @param messageType the type of message (typically "current_task")
     * @param data the JSON data containing task information
     */
    public TaskMessage(String messageType, JsonNode data) {
        super(messageType, data);
    }
    
    /**
     * Gets the task status.
     * 
     * @return The task status
     */
    public String getStatus() {
        if (data instanceof JsonNode) {
            return ((JsonNode) data).path("status").asText();
        }
        return null;
    }
}
