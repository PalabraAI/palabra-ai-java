package ai.palabra.base;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import java.util.Iterator;

/**
 * Message containing audio data (TTS output).
 */
public class AudioMessage extends Message {
    private final byte[] audioData;
    
    /**
     * Creates a new audio message from WebSocket data.
     * 
     * @param messageType the type of message (e.g., "output_audio_data")
     * @param data the JSON data containing Base64-encoded audio
     */
    public AudioMessage(String messageType, JsonNode data) {
        super(messageType, data);
        
        // Temporary debug logging to see the structure
        boolean debugMode = System.getProperty("PALABRA_DEBUG_AUDIO", "false").equals("true");
        
        if (debugMode) {
            System.out.println("🔍 AudioMessage DEBUG - messageType: " + messageType);
            System.out.println("🔍 AudioMessage DEBUG - data type: " + data.getNodeType());
            System.out.println("🔍 AudioMessage DEBUG - data isObject: " + data.isObject());
            System.out.println("🔍 AudioMessage DEBUG - data isTextual: " + data.isTextual());
        }
        
        byte[] tempAudioData = new byte[0];
        
        // If data is textual, it might be a JSON string we need to parse
        if (data.isTextual()) {
            if (debugMode) {
                System.out.println("🔍 AudioMessage DEBUG - data is textual, parsing as JSON string");
            }
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                JsonNode actualData = mapper.readTree(data.asText());
                
                if (debugMode) {
                    Iterator<String> actualFieldNames = actualData.fieldNames();
                    System.out.print("🔍 AudioMessage DEBUG - parsed data keys: ");
                    while (actualFieldNames.hasNext()) {
                        System.out.print(actualFieldNames.next() + " ");
                    }
                    System.out.println();
                }
                
                if (actualData.has("data")) {
                    String base64Audio = actualData.path("data").asText();
                    if (debugMode) {
                        System.out.println("🔍 AudioMessage DEBUG - Found base64 data, length: " + base64Audio.length() + " chars");
                    }
                    try {
                        tempAudioData = Base64.getDecoder().decode(base64Audio);
                        if (debugMode) {
                            System.out.println("🔍 AudioMessage DEBUG - Decoded audio data: " + tempAudioData.length + " bytes");
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("🔍 AudioMessage DEBUG - Failed to decode base64: " + e.getMessage());
                        throw new RuntimeException("Invalid base64 audio data", e);
                    }
                } else if (debugMode) {
                    System.out.println("🔍 AudioMessage DEBUG - No 'data' field in parsed JSON");
                }
            } catch (Exception e) {
                System.err.println("🔍 AudioMessage DEBUG - Failed to parse JSON string: " + e.getMessage());
            }
        } else if (data.has("data")) {
            String base64Audio = data.path("data").asText();
            if (debugMode) {
                System.out.println("🔍 AudioMessage DEBUG - Found base64 data directly, length: " + base64Audio.length() + " chars");
            }
            try {
                tempAudioData = Base64.getDecoder().decode(base64Audio);
                if (debugMode) {
                    System.out.println("🔍 AudioMessage DEBUG - Decoded audio data: " + tempAudioData.length + " bytes");
                }
            } catch (IllegalArgumentException e) {
                System.err.println("🔍 AudioMessage DEBUG - Failed to decode base64: " + e.getMessage());
                throw new RuntimeException("Invalid base64 audio data", e);
            }
        } else if (debugMode) {
            System.out.println("🔍 AudioMessage DEBUG - No 'data' field found");
        }
        
        this.audioData = tempAudioData;
    }
    
    /**
     * Gets the decoded audio data.
     * 
     * @return The audio data as bytes
     */
    public byte[] getAudioData() {
        return audioData.clone(); // Return a copy for safety
    }
    
    /**
     * Gets the size of the audio data.
     * 
     * @return The number of audio bytes
     */
    public int getAudioSize() {
        return audioData.length;
    }
    
    @Override
    public String toString() {
        return String.format("AudioMessage{size=%d bytes}", audioData.length);
    }
}
