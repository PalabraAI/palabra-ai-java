package ai.palabra.base;

import ai.palabra.Language;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message containing transcription data.
 * Handles both partial and final transcriptions.
 */
public class TranscriptionMessage extends Message {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionMessage.class);
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        // Configure Jackson to not escape non-ASCII characters
        objectMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }
    
    private final String transcriptionId;
    private final Language language;
    private final String text;
    private final boolean isPartial;
    
    /**
     * Creates a new transcription message from WebSocket data.
     * 
     * @param messageType the type of message ("partial_transcription" or "final_transcription")
     * @param data the JSON data containing transcription details
     */
    public TranscriptionMessage(String messageType, JsonNode data) {
        super(messageType, data);
        
        this.isPartial = "partial_transcription".equals(messageType);
        
        // Parse transcription data and assign values
        String tempTranscriptionId = null;
        String tempText = null;
        Language tempLanguage = null;
        
        try {
            JsonNode transcriptionData;
            
            // Handle case where data is a JSON string (needs parsing)
            if (data.isTextual()) {
                String dataStr = data.asText();
                transcriptionData = objectMapper.readTree(dataStr);
            } else {
                transcriptionData = data;
            }
            
            if (transcriptionData.has("transcription")) {
                JsonNode transcription = transcriptionData.path("transcription");
                tempTranscriptionId = transcription.path("transcription_id").asText();
                tempText = transcription.path("text").asText();
                
                // Parse language
                String langCode = transcription.path("language").asText();
                if (langCode != null && !langCode.isEmpty()) {
                    try {
                        tempLanguage = Language.fromCode(langCode);
                    } catch (IllegalArgumentException e) {
                        // Fallback: try to find language by simple code
                        try {
                            tempLanguage = Language.fromSimpleCode(langCode);
                        } catch (IllegalArgumentException e2) {
                            throw new RuntimeException("Unsupported language code: " + langCode, e2);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // Re-throw RuntimeException to let test assertions catch them
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse transcription message data: {}", data, e);
        }
        
        // Final assignment
        this.transcriptionId = tempTranscriptionId;
        this.text = tempText;
        this.language = tempLanguage;
    }
    
    /**
     * Gets the transcription ID.
     * 
     * @return The transcription ID
     */
    public String getTranscriptionId() {
        return transcriptionId;
    }
    
    /**
     * Gets the transcribed text.
     * 
     * @return The transcribed text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets the language of the transcription.
     * 
     * @return The language
     */
    public Language getLanguage() {
        return language;
    }
    
    /**
     * Checks if this is a partial transcription.
     * 
     * @return true if partial, false if final
     */
    public boolean isPartial() {
        return isPartial;
    }
    
    /**
     * Gets the deduplication key for this message, similar to Python implementation.
     * 
     * @return The deduplication key
     */
    public String getDeduplicationKey() {
        return transcriptionId + " " + toString();
    }
    
    @Override
    public String toString() {
        return String.format("TranscriptionMessage{id='%s', lang='%s', text='%s', partial=%s}", 
                           transcriptionId, language != null ? language.getCode() : null, text, isPartial);
    }
}
