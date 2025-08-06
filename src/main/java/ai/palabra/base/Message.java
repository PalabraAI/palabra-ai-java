package ai.palabra.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Base class for all Palabra AI messages.
 * Handles message parsing and type detection similar to the Python implementation.
 */
public abstract class Message {
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        // Configure Jackson to not escape non-ASCII characters
        objectMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }
    
    /** The type of this message (e.g., "output_audio_data", "transcription"). */
    protected final String messageType;
    
    /** The data payload of this message. */
    protected final Object data;
    
    /**
     * Creates a new message with the specified type and data.
     * 
     * @param messageType the type of message
     * @param data the message data payload
     */
    protected Message(String messageType, Object data) {
        this.messageType = messageType;
        this.data = data;
    }
    
    /**
     * Gets the message type.
     * 
     * @return The message type
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * Gets the message data.
     * 
     * @return The message data
     */
    public Object getData() {
        return data;
    }
    
    /**
     * Creates a Message from raw data, similar to Python Message.decode().
     * 
     * @param rawData The raw message data
     * @return A Message instance
     */
    public static Message fromRawData(Object rawData) {
        try {
            if (rawData instanceof String) {
                JsonNode jsonNode = objectMapper.readTree((String) rawData);
                return parseMessageFromJson(jsonNode);
            } else if (rawData instanceof Map) {
                JsonNode jsonNode = objectMapper.valueToTree(rawData);
                return parseMessageFromJson(jsonNode);
            } else {
                JsonNode jsonNode = objectMapper.valueToTree(rawData);
                return parseMessageFromJson(jsonNode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message from raw data", e);
        }
    }
    
    private static Message parseMessageFromJson(JsonNode jsonNode) {
        String messageType = jsonNode.path("message_type").asText();
        
        switch (messageType) {
            case "current_task":
                return new TaskMessage(messageType, jsonNode.path("data"));
            case "partial_transcription":
            case "final_transcription":
                return new TranscriptionMessage(messageType, jsonNode.path("data"));
            case "output_audio_data":
                return new AudioMessage(messageType, jsonNode.path("data"));
            case "error":
                return new ErrorMessage(messageType, jsonNode.path("data"));
            default:
                return new GenericMessage(messageType, jsonNode.path("data"));
        }
    }
    
    @Override
    public String toString() {
        return String.format("Message{type='%s', data=%s}", messageType, data);
    }
}
