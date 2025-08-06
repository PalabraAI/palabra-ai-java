package ai.palabra.config;

import ai.palabra.Language;
import ai.palabra.adapter.FileReader;
import ai.palabra.adapter.FileWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for all configuration classes in the config package.
 */
class ConfigurationClassesTest {
    
    @Test
    void testAudioFormatBuilder() {
        AudioFormat audioFormat = AudioFormat.builder()
            .format("pcm_s16le")
            .sampleRate(48000)
            .channels(2)
            .build();
        
        assertEquals("pcm_s16le", audioFormat.getFormat());
        assertEquals(48000, audioFormat.getSampleRate());
        assertEquals(2, audioFormat.getChannels());
    }
    
    @Test
    void testAudioFormatDefaults() {
        AudioFormat audioFormat = AudioFormat.builder().build();
        
        // Should have default values
        assertNotNull(audioFormat.getFormat());
        assertTrue(audioFormat.getSampleRate() > 0);
        assertTrue(audioFormat.getChannels() > 0);
    }
    
    @Test
    void testInputStreamConfigBuilder() {
        AudioFormat audioFormat = AudioFormat.builder()
            .format("pcm_s16le")
            .sampleRate(48000)
            .channels(1)
            .build();
        
        InputStreamConfig inputStream = InputStreamConfig.builder()
            .contentType("audio")
            .audioFormat(audioFormat)
            .build();
        
        assertEquals("audio", inputStream.getContentType());
        assertNotNull(inputStream.getSource());
        assertEquals("pcm_s16le", inputStream.getSource().get("format"));
        assertEquals(48000, inputStream.getSource().get("sample_rate"));
        assertEquals(1, inputStream.getSource().get("channels"));
    }
    
    @Test
    void testInputStreamConfigWebsocket() {
        InputStreamConfig inputStream = InputStreamConfig.websocket();
        
        assertEquals("ws", inputStream.getContentType()); // Content type is "ws" for WebSocket
        assertNotNull(inputStream.getSource());
        assertEquals("ws", inputStream.getSource().get("type"));
    }
    
    @Test
    void testInputStreamConfigDefaults() {
        InputStreamConfig defaults = InputStreamConfig.defaults();
        
        assertNotNull(defaults);
        assertEquals("ws", defaults.getContentType()); // WebSocket content type
        assertNotNull(defaults.getSource());
    }
    
    @Test
    void testOutputStreamConfigBuilder() {
        OutputStreamConfig outputStream = OutputStreamConfig.builder()
            .contentType("ws")
            .format("pcm_s16le")
            .build();
        
        assertEquals("ws", outputStream.getContentType());
        assertNotNull(outputStream.getTarget());
        assertEquals("pcm_s16le", outputStream.getTarget().get("format"));
    }
    
    @Test
    void testOutputStreamConfigWebsocket() {
        OutputStreamConfig outputStream = OutputStreamConfig.websocket("zlib_pcm_s16le");
        
        assertEquals("ws", outputStream.getContentType());
        assertNotNull(outputStream.getTarget());
        assertEquals("zlib_pcm_s16le", outputStream.getTarget().get("format"));
    }
    
    @Test
    void testOutputStreamConfigDefaults() {
        OutputStreamConfig defaults = OutputStreamConfig.defaults();
        
        assertNotNull(defaults);
        assertEquals("ws", defaults.getContentType());
        assertNotNull(defaults.getTarget());
    }
    
    @Test
    void testOutputStreamConfigValidation() {
        // Test invalid format validation - this is runtime validation in setter, so IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
            OutputStreamConfig.builder()
                .format("invalid_format")
                .build());
    }
    
    @Test
    void testSourceLangConfigBuilder() {
        TranscriptionConfig transcription = TranscriptionConfig.builder()
            .asrModel("whisper-large")
            .build();
        
        SourceLangConfig source = SourceLangConfig.builder()
            .language(Language.EN_US)
            .reader(new FileReader("input.wav"))
            .transcription(transcription)
            .build();
        
        assertEquals(Language.EN_US, source.getLanguage());
        assertNotNull(source.getReader());
        assertNotNull(source.getTranscription());
        assertEquals("auto", source.getTranscription().getAsrModel()); // Default ASR model is "auto"
    }
    
    @Test
    void testSourceLangConfigValidation() {
        // Test missing language - this is build-time validation, so IllegalStateException
        assertThrows(IllegalStateException.class, () ->
            SourceLangConfig.builder()
                .reader(new FileReader("input.wav"))
                .build());
    }
    
    @Test
    void testTargetLangConfigBuilder() {
        TranslationConfig translation = TranslationConfig.builder()
            .targetLanguage("es_MX")
            .translatePartialTranscriptions(true)
            .build();
        
        TargetLangConfig target = TargetLangConfig.builder()
            .language(Language.ES_MX)
            .writer(new FileWriter("output.wav"))
            .translation(translation)
            .build();
        
        assertEquals(Language.ES_MX, target.getLanguage());
        assertNotNull(target.getWriter());
        assertNotNull(target.getTranslation());
        assertTrue(target.getTranslation().isTranslatePartialTranscriptions());
    }
    
    @Test
    void testTargetLangConfigValidation() {
        // Test missing language - this is build-time validation, so IllegalStateException
        assertThrows(IllegalStateException.class, () ->
            TargetLangConfig.builder()
                .writer(new FileWriter("output.wav"))
                .build());
    }
    
    @Test
    void testTranscriptionConfigBuilder() {
        SentenceSplitterConfig sentenceSplitter = SentenceSplitterConfig.builder()
            .enabled(true)
            .build();
        
        TranscriptionConfig transcription = TranscriptionConfig.builder()
            .sourceLanguage("en_US")
            .detectableLanguages(Arrays.asList("en_US", "es_MX", "fr_FR"))
            .asrModel("whisper-large")
            .denoise("rnnoise")
            .allowHotwordsGlossaries(false)
            .suppressNumeralTokens(true)
            .diarizeSpeakers(true)
            .priority("high")
            .minAlignmentScore(0.7f)
            .maxAlignmentCer(0.3f)
            .segmentConfirmationSilenceThreshold(0.8f)
            .onlyConfirmBySilence(false)
            .batchedInference(true)
            .forceDetectLanguage(false)
            .calculateVoiceLoudness(true)
            .sentenceSplitter(sentenceSplitter)
            .build();
        
        assertEquals("en_US", transcription.getSourceLanguage());
        assertEquals(3, transcription.getDetectableLanguages().size());
        assertEquals("whisper-large", transcription.getAsrModel());
        assertEquals("rnnoise", transcription.getDenoise());
        assertFalse(transcription.isAllowHotwordsGlossaries());
        assertTrue(transcription.isSuppressNumeralTokens());
        assertTrue(transcription.isDiarizeSpeakers());
        assertEquals("high", transcription.getPriority());
        assertEquals(0.7f, transcription.getMinAlignmentScore());
        assertEquals(0.3f, transcription.getMaxAlignmentCer());
        assertEquals(0.8f, transcription.getSegmentConfirmationSilenceThreshold());
        assertFalse(transcription.isOnlyConfirmBySilence());
        assertTrue(transcription.isBatchedInference());
        assertFalse(transcription.isForceDetectLanguage());
        assertTrue(transcription.isCalculateVoiceLoudness());
        assertNotNull(transcription.getSentenceSplitter());
    }
    
    @Test
    void testTranslationConfigBuilder() {
        SpeechGenerationConfig speechGen = SpeechGenerationConfig.builder()
            .ttsModel("tacotron2")
            .voiceId("voice-001")
            .build();
        
        TranslationConfig translation = TranslationConfig.builder()
            .targetLanguage("es_MX")
            .allowedSourceLanguages(Arrays.asList("en_US", "en_GB"))
            .translationModel("opus-mt")
            .allowTranslationGlossaries(false)
            .style("formal")
            .translatePartialTranscriptions(true)
            .speechGeneration(speechGen)
            .build();
        
        assertEquals("es_MX", translation.getTargetLanguage());
        assertEquals(2, translation.getAllowedSourceLanguages().size());
        assertEquals("opus-mt", translation.getTranslationModel());
        assertFalse(translation.isAllowTranslationGlossaries());
        assertEquals("formal", translation.getStyle());
        assertTrue(translation.isTranslatePartialTranscriptions());
        assertNotNull(translation.getSpeechGeneration());
        assertEquals("tacotron2", translation.getSpeechGeneration().getTtsModel());
    }
    
    @Test
    void testSpeechGenerationConfigBuilder() {
        VoiceTimbreDetectionConfig voiceTimbre = VoiceTimbreDetectionConfig.builder()
            .enabled(true)
            .build();
        
        SpeechGenerationConfig.TTSAdvancedConfig advanced = 
            SpeechGenerationConfig.TTSAdvancedConfig.builder()
                .f0VarianceFactor(1.2f)
                .energyVarianceFactor(0.8f)
                .withCustomStress(true)
                .build();
        
        SpeechGenerationConfig speechGen = SpeechGenerationConfig.builder()
            .ttsModel("tacotron2")
            .voiceCloning(true)
            .voiceCloningMode("dynamic")
            .denoiseVoiceSamples(false)
            .voiceId("voice-001")
            .voiceTimbreDetection(voiceTimbre)
            .speechTempoAuto(false)
            .speechTempoTimingsFactor(2)
            .speechTempoAdjustmentFactor(1.1f)
            .advanced(advanced)
            .build();
        
        assertEquals("tacotron2", speechGen.getTtsModel());
        assertTrue(speechGen.isVoiceCloning());
        assertEquals("dynamic", speechGen.getVoiceCloningMode());
        assertFalse(speechGen.isDenoiseVoiceSamples());
        assertEquals("voice-001", speechGen.getVoiceId());
        assertNotNull(speechGen.getVoiceTimbreDetection());
        assertFalse(speechGen.isSpeechTempoAuto());
        assertEquals(2, speechGen.getSpeechTempoTimingsFactor());
        assertEquals(1.1f, speechGen.getSpeechTempoAdjustmentFactor());
        assertNotNull(speechGen.getAdvanced());
        assertEquals(1.2f, speechGen.getAdvanced().getF0VarianceFactor());
        assertEquals(0.8f, speechGen.getAdvanced().getEnergyVarianceFactor());
        assertTrue(speechGen.getAdvanced().isWithCustomStress());
    }
    
    @Test
    void testSentenceSplitterConfigBuilder() {
        SentenceSplitterConfig splitter = SentenceSplitterConfig.builder()
            .enabled(true)
            .build();
        
        assertTrue(splitter.isEnabled());
    }
    
    @Test
    void testVoiceTimbreDetectionConfigBuilder() {
        VoiceTimbreDetectionConfig voiceTimbre = VoiceTimbreDetectionConfig.builder()
            .enabled(true)
            .build();
        
        assertTrue(voiceTimbre.isEnabled());
    }
    
    @Test
    void testTranslationQueueConfigsBuilder() {
        QueueConfig queueConfig = QueueConfig.builder()
            .desiredQueueLevelMs(500)
            .maxQueueLevelMs(1000)
            .build();
        
        TranslationQueueConfigs queueConfigs = TranslationQueueConfigs.builder()
            .global(queueConfig)
            .addLanguageConfig("en_US", queueConfig)
            .build();
        
        assertNotNull(queueConfigs.getGlobal());
        assertEquals(500, queueConfigs.getGlobal().getDesiredQueueLevelMs());
        assertEquals(1000, queueConfigs.getGlobal().getMaxQueueLevelMs());
        assertNotNull(queueConfigs.getPerLanguage());
        assertEquals(queueConfig, queueConfigs.getForLanguage("en_US"));
    }
    
    @Test
    void testConfigImmutability() {
        // Test that config objects return defensive copies of mutable collections
        List<String> originalList = new ArrayList<>(Arrays.asList("lang1", "lang2"));
        
        TranscriptionConfig transcription = TranscriptionConfig.builder()
            .detectableLanguages(originalList)
            .build();
        
        List<String> returnedList = transcription.getDetectableLanguages();
        
        // Modify the original input list - this should not affect the config
        originalList.add("lang3");
        
        // The returned list should not be affected by original list modification
        assertEquals(2, returnedList.size());
        
        // The returned list should be a defensive copy, not the same object
        assertNotSame(originalList, returnedList);
        
        // Try to modify the returned list - this should not affect the config
        List<String> secondReturnedList = transcription.getDetectableLanguages();
        returnedList.add("lang4"); // This modifies the returned copy, not the internal state
        
        // Getting the list again should return a fresh copy without the modification
        List<String> thirdReturnedList = transcription.getDetectableLanguages();
        assertEquals(2, thirdReturnedList.size());
        assertFalse(thirdReturnedList.contains("lang4"));
    }
    
    @Test
    void testBuilderPatternConsistency() {
        // Test that all builders follow consistent patterns
        
        // Test AudioFormat builder with valid format
        AudioFormat.Builder audioBuilder = AudioFormat.builder();
        assertSame(audioBuilder, audioBuilder.format("pcm_s16le"));
        assertSame(audioBuilder, audioBuilder.sampleRate(44100));
        assertSame(audioBuilder, audioBuilder.channels(2));
        
        // Test InputStreamConfig builder
        InputStreamConfig.Builder inputBuilder = InputStreamConfig.builder();
        assertSame(inputBuilder, inputBuilder.contentType("ws"));
        
        // Test TranscriptionConfig builder with valid ASR model
        TranscriptionConfig.Builder transcBuilder = TranscriptionConfig.builder();
        assertSame(transcBuilder, transcBuilder.asrModel("whisper-large"));
        assertSame(transcBuilder, transcBuilder.allowHotwordsGlossaries(true));
        
        // All builders should produce valid objects (need to set required fields)
        assertNotNull(audioBuilder.format("pcm_s16le").build());
        assertNotNull(inputBuilder.build());
        assertNotNull(transcBuilder.build());
    }
}