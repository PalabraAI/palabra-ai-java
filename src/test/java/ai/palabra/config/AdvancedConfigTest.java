package ai.palabra.config;

import ai.palabra.Language;
import ai.palabra.adapter.FileReader;
import ai.palabra.adapter.FileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Arrays;

/**
 * Comprehensive tests for AdvancedConfig class and all related configuration classes.
 */
class AdvancedConfigTest {
    
    @Test
    void testAdvancedConfigBuilder() {
        AdvancedConfig config = AdvancedConfig.builder()
            .source(SourceLangConfig.builder()
                .language(Language.EN_US)
                .reader(new FileReader("input.wav"))
                .build())
            .addTarget(TargetLangConfig.builder()
                .language(Language.ES_MX)
                .writer(new FileWriter("output.wav"))
                .build())
            .build();
        
        assertNotNull(config.getSource());
        assertEquals(Language.EN_US, config.getSource().getLanguage());
        assertEquals(1, config.getTargets().size());
        assertEquals(Language.ES_MX, config.getTargets().get(0).getLanguage());
    }
    
    @Test
    void testAdvancedConfigWithMultipleTargets() {
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output_es.wav"))
            .addTarget(Language.FR, new FileWriter("output_fr.wav"))
            .addTarget(Language.DE, new FileWriter("output_de.wav"))
            .build();
        
        assertEquals(3, config.getTargets().size());
        assertEquals(Language.ES_MX, config.getTargets().get(0).getLanguage());
        assertEquals(Language.FR, config.getTargets().get(1).getLanguage());
        assertEquals(Language.DE, config.getTargets().get(2).getLanguage());
    }
    
    @Test
    void testAdvancedConfigWithStreamConfigurations() {
        InputStreamConfig inputStream = InputStreamConfig.builder()
            .contentType("audio")
            .audioFormat(AudioFormat.builder()
                .format("pcm_s16le")
                .sampleRate(48000)
                .channels(1)
                .build())
            .build();
            
        OutputStreamConfig outputStream = OutputStreamConfig.builder()
            .contentType("audio")
            .format("pcm_s16le")
            .build();
        
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .inputStream(inputStream)
            .outputStream(outputStream)
            .build();
        
        assertNotNull(config.getInputStream());
        assertNotNull(config.getOutputStream());
        assertEquals("audio", config.getInputStream().getContentType()); // Set explicitly to "audio" in test
        assertEquals("audio", config.getOutputStream().getContentType()); // Set explicitly to "audio" in test
    }
    
    @Test
    void testAdvancedConfigWithTranscriptionSettings() {
        TranscriptionConfig transcription = TranscriptionConfig.builder()
            .sourceLanguage("en_US") // Set source language to prevent override
            .asrModel("whisper-large")
            .segmentConfirmationSilenceThreshold(0.8f)
            .allowHotwordsGlossaries(false)
            .diarizeSpeakers(true)
            .build();
        
        SourceLangConfig source = SourceLangConfig.builder()
            .language(Language.EN_US)
            .reader(new FileReader("input.wav"))
            .transcription(transcription)
            .build();
        
        AdvancedConfig config = AdvancedConfig.builder()
            .source(source)
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        assertNotNull(config.getSource().getTranscription());
        assertEquals("whisper-large", config.getSource().getTranscription().getAsrModel());
        assertEquals(0.8f, config.getSource().getTranscription().getSegmentConfirmationSilenceThreshold());
        assertFalse(config.getSource().getTranscription().isAllowHotwordsGlossaries());
        assertTrue(config.getSource().getTranscription().isDiarizeSpeakers());
    }
    
    @Test
    void testAdvancedConfigWithSpeechGeneration() {
        SpeechGenerationConfig speechGen = SpeechGenerationConfig.builder()
            .ttsModel("tacotron2")
            .voiceId("voice-001")
            .voiceCloning(true)
            .voiceCloningMode("dynamic")
            .build();
        
        TranslationConfig translation = TranslationConfig.builder()
            .targetLanguage("es_MX")
            .speechGeneration(speechGen)
            .translatePartialTranscriptions(true)
            .build();
        
        TargetLangConfig target = TargetLangConfig.builder()
            .language(Language.ES_MX)
            .writer(new FileWriter("output.wav"))
            .translation(translation)
            .build();
        
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(target)
            .build();
        
        TargetLangConfig targetConfig = config.getTargets().get(0);
        assertNotNull(targetConfig.getTranslation());
        assertNotNull(targetConfig.getTranslation().getSpeechGeneration());
        assertEquals("tacotron2", targetConfig.getTranslation().getSpeechGeneration().getTtsModel());
        assertEquals("voice-001", targetConfig.getTranslation().getSpeechGeneration().getVoiceId());
        assertTrue(targetConfig.getTranslation().getSpeechGeneration().isVoiceCloning());
        assertTrue(targetConfig.getTranslation().isTranslatePartialTranscriptions());
    }
    
    @Test
    void testAdvancedConfigValidation() {
        // Test missing source - build-time validation throws IllegalStateException
        assertThrows(IllegalStateException.class, () ->
            AdvancedConfig.builder()
                .addTarget(TargetLangConfig.builder()
                    .language(Language.ES_MX)
                    .build())
                .build());
        
        // Test missing targets - build-time validation throws IllegalStateException
        assertThrows(IllegalStateException.class, () ->
            AdvancedConfig.builder()
                .source(SourceLangConfig.builder()
                    .language(Language.EN_US)
                    .build())
                .build());
    }
    
    @Test
    void testAdvancedConfigWithAllowedMessageTypes() {
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .allowedMessageTypes(Arrays.asList("transcription", "translation", "audio"))
            .build();
        
        assertEquals(3, config.getAllowedMessageTypes().size());
        assertTrue(config.getAllowedMessageTypes().contains("transcription"));
        assertTrue(config.getAllowedMessageTypes().contains("translation"));
        assertTrue(config.getAllowedMessageTypes().contains("audio"));
    }
    
    @Test
    void testAdvancedConfigSettings() {
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .silent(true)
            .debug(false)
            .timeout(30000)
            .build();
        
        assertTrue(config.isSilent());
        assertFalse(config.isDebug());
        assertEquals(30000, config.getTimeout());
    }
    
    @Test
    void testJsonSerialization(@TempDir Path tempDir) throws IOException {
        // Create simpler advanced config for JSON testing (focus on core serialization)
        AdvancedConfig originalConfig = AdvancedConfig.builder()
            .source(SourceLangConfig.builder()
                .language(Language.EN_US)
                .transcription(TranscriptionConfig.builder()
                    .sourceLanguage("en_US") // Set source language to prevent override
                    .asrModel("whisper-large")
                    .segmentConfirmationSilenceThreshold(0.9f)
                    .build())
                .build())
            .addTarget(TargetLangConfig.builder()
                .language(Language.ES_MX)
                .translation(TranslationConfig.builder()
                    .targetLanguage("es_MX")
                    .speechGeneration(SpeechGenerationConfig.builder()
                        .ttsModel("tacotron2")
                        .voiceId("voice-001")
                        .build())
                    .build())
                .build())
            .build(); // Use default values for silent, debug, timeout
        
        // Test JSON string serialization (Reader/Writer are excluded with @JsonIgnore)
        String json = originalConfig.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("source"));
        assertTrue(json.contains("targets"));
        
        // Test JSON string deserialization
        AdvancedConfig loadedConfig = AdvancedConfig.fromJson(json);
        
        // Verify core structure
        assertEquals(Language.EN_US, loadedConfig.getSource().getLanguage());
        assertEquals(1, loadedConfig.getTargets().size());
        assertEquals(Language.ES_MX, loadedConfig.getTargets().get(0).getLanguage());
        
        // Verify transcription settings
        assertNotNull(loadedConfig.getSource().getTranscription());
        assertEquals("whisper-large", loadedConfig.getSource().getTranscription().getAsrModel());
        assertEquals(0.9f, loadedConfig.getSource().getTranscription().getSegmentConfirmationSilenceThreshold());
        
        // Verify translation settings
        TargetLangConfig target = loadedConfig.getTargets().get(0);
        assertNotNull(target.getTranslation());
        assertNotNull(target.getTranslation().getSpeechGeneration());
        assertEquals("tacotron2", target.getTranslation().getSpeechGeneration().getTtsModel());
        assertEquals("voice-001", target.getTranslation().getSpeechGeneration().getVoiceId());
        
        // Note: Reader/Writer objects are not included in JSON serialization by design (@JsonIgnore)
        // They need to be set separately after deserialization for actual usage
    }
    
    @Test
    void testJsonStringConversion() throws IOException {
        // Create config without Reader/Writer for JSON serialization testing
        AdvancedConfig config = AdvancedConfig.builder()
            .source(SourceLangConfig.builder()
                .language(Language.EN_US)
                .build())
            .addTarget(TargetLangConfig.builder()
                .language(Language.ES_MX)
                .build())
            .build();
        
        // Convert to JSON string
        String json = config.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("source"));
        assertTrue(json.contains("targets"));
        
        // Convert back from JSON string
        AdvancedConfig loadedConfig = AdvancedConfig.fromJson(json);
        assertEquals(Language.EN_US, loadedConfig.getSource().getLanguage());
        assertEquals(1, loadedConfig.getTargets().size());
        assertEquals(Language.ES_MX, loadedConfig.getTargets().get(0).getLanguage());
        
        // Note: Reader/Writer need to be set after JSON deserialization for actual usage
    }
    
    @Test
    void testDefaultStreamConfigurations() {
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        // Should have default stream configurations
        assertNotNull(config.getInputStream());
        assertNotNull(config.getOutputStream());
        assertEquals("ws", config.getInputStream().getContentType());
        assertEquals("ws", config.getOutputStream().getContentType());
    }
    
    @Test
    void testImmutability() {
        AdvancedConfig config = AdvancedConfig.builder()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        // Test that returned lists are defensive copies
        var targets = config.getTargets();
        int originalSize = targets.size();
        
        // Try to modify the returned list - this should either be immutable or a defensive copy
        targets.add(TargetLangConfig.builder()
            .language(Language.FR)
            .writer(new FileWriter("test.wav"))
            .build());
        
        // Verify the config's internal targets are unchanged by getting a fresh copy
        assertEquals(originalSize, config.getTargets().size());
        
        // The modification should not affect the config's internal state
        assertFalse(config.getTargets().stream()
            .anyMatch(target -> target.getLanguage() == Language.FR));
    }
}