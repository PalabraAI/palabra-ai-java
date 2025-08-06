package ai.palabra.base;

import ai.palabra.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Message and its subclasses.
 */
class MessageTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void testGenericMessageCreation() {
        GenericMessage message = new GenericMessage("test_type", null);
        
        assertEquals("test_type", message.getMessageType());
        assertNull(message.getData());
    }
    
    @Test
    void testTaskMessageFromJson() {
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("status", "confirmed");
        
        TaskMessage message = new TaskMessage("current_task", dataNode);
        
        assertEquals("current_task", message.getMessageType());
        assertEquals("confirmed", message.getStatus());
    }
    
    @Test
    void testTranscriptionMessageFromJson() {
        ObjectNode transcriptionNode = objectMapper.createObjectNode();
        transcriptionNode.put("transcription_id", "trans_123");
        transcriptionNode.put("language", "en-US");
        transcriptionNode.put("text", "Hello world");
        
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("transcription", transcriptionNode);
        
        TranscriptionMessage message = new TranscriptionMessage("final_transcription", dataNode);
        
        assertEquals("final_transcription", message.getMessageType());
        assertEquals("trans_123", message.getTranscriptionId());
        assertEquals("Hello world", message.getText());
        assertEquals(Language.EN_US, message.getLanguage());
        assertFalse(message.isPartial());
    }
    
    @Test
    void testPartialTranscriptionMessage() {
        ObjectNode transcriptionNode = objectMapper.createObjectNode();
        transcriptionNode.put("transcription_id", "trans_456");
        transcriptionNode.put("language", "es-MX");
        transcriptionNode.put("text", "Hola");
        
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("transcription", transcriptionNode);
        
        TranscriptionMessage message = new TranscriptionMessage("partial_transcription", dataNode);
        
        assertEquals("partial_transcription", message.getMessageType());
        assertTrue(message.isPartial());
        assertEquals(Language.ES_MX, message.getLanguage());
    }
    
    @Test
    void testTranscriptionMessageDeduplicationKey() {
        ObjectNode transcriptionNode = objectMapper.createObjectNode();
        transcriptionNode.put("transcription_id", "trans_123");
        transcriptionNode.put("language", "en-US");
        transcriptionNode.put("text", "Test");
        
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("transcription", transcriptionNode);
        
        TranscriptionMessage message = new TranscriptionMessage("final_transcription", dataNode);
        
        String dedupKey = message.getDeduplicationKey();
        assertNotNull(dedupKey);
        assertTrue(dedupKey.contains("trans_123"));
    }
    
    @Test
    void testAudioMessageFromJson() {
        byte[] testAudio = {1, 2, 3, 4, 5};
        String base64Audio = Base64.getEncoder().encodeToString(testAudio);
        
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("data", base64Audio);
        
        AudioMessage message = new AudioMessage("output_audio_data", dataNode);
        
        assertEquals("output_audio_data", message.getMessageType());
        assertArrayEquals(testAudio, message.getAudioData());
        assertEquals(5, message.getAudioSize());
    }
    
    @Test
    void testAudioMessageWithInvalidBase64() {
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("data", "invalid-base64!");
        
        assertThrows(RuntimeException.class, () -> {
            new AudioMessage("output_audio_data", dataNode);
        });
    }
    
    @Test
    void testErrorMessageFromJson() {
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("error", "Invalid request");
        dataNode.put("details", "Missing required field");
        
        ErrorMessage message = new ErrorMessage("error", dataNode);
        
        assertEquals("error", message.getMessageType());
        assertEquals("Invalid request", message.getError());
        assertEquals("Missing required field", message.getDetails());
    }
    
    @Test
    void testMessageFromRawDataWithString() {
        String jsonString = """
            {
                "message_type": "current_task",
                "data": {
                    "status": "confirmed"
                }
            }
            """;
        
        Message message = Message.fromRawData(jsonString);
        
        assertInstanceOf(TaskMessage.class, message);
        assertEquals("current_task", message.getMessageType());
    }
    
    @Test
    void testMessageFromRawDataWithMap() {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("message_type", "error");
        
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("error", "Test error");
        messageMap.put("data", dataMap);
        
        Message message = Message.fromRawData(messageMap);
        
        assertInstanceOf(ErrorMessage.class, message);
        assertEquals("error", message.getMessageType());
    }
    
    @Test
    void testMessageFromRawDataWithUnknownType() {
        String jsonString = """
            {
                "message_type": "unknown_type",
                "data": {
                    "test": "value"
                }
            }
            """;
        
        Message message = Message.fromRawData(jsonString);
        
        assertInstanceOf(GenericMessage.class, message);
        assertEquals("unknown_type", message.getMessageType());
    }
    
    @Test
    void testTranscriptionMessageWithUnsupportedLanguage() {
        ObjectNode transcriptionNode = objectMapper.createObjectNode();
        transcriptionNode.put("transcription_id", "trans_123");
        transcriptionNode.put("language", "xx-XX"); // Invalid language code
        transcriptionNode.put("text", "Test");
        
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("transcription", transcriptionNode);
        
        assertThrows(RuntimeException.class, () -> {
            new TranscriptionMessage("final_transcription", dataNode);
        });
    }
    
    @Test
    void testMessageToString() {
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("test", "value");
        
        GenericMessage message = new GenericMessage("test_type", dataNode);
        
        String toString = message.toString();
        assertTrue(toString.contains("test_type"));
        assertTrue(toString.contains("Message"));
    }
}
