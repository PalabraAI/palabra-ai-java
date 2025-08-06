package ai.palabra;

import ai.palabra.adapter.FileReader;
import ai.palabra.adapter.FileWriter;
import ai.palabra.PalabraException;
import ai.palabra.internal.PalabraRESTClient;
import ai.palabra.internal.SessionCredentials;
import ai.palabra.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for PalabraAI client.
 */
class PalabraAITest {

    private PalabraAI client;
    private Config validConfig;

    @BeforeEach
    void setUp() {
        client = new PalabraAI("test-client-id", "test-client-secret");
        
        // Create a valid configuration for testing
        validConfig = Config.builder()
                .sourceLang(Language.EN_US)
                .targetLang(Language.ES_MX)
                .reader(new FileReader("test-input.wav"))
                .writer(new FileWriter("test-output.wav"))
                .build();
    }

    @Test
    void testConstructorWithValidCredentials() {
        PalabraAI testClient = new PalabraAI("client-id", "client-secret");
        assertEquals("client-id", testClient.getClientId());
        assertEquals("client-secret", testClient.getClientSecret());
        assertEquals("https://api.palabra.ai", testClient.getApiEndpoint());
    }

    @Test
    void testConstructorWithCustomEndpoint() {
        PalabraAI testClient = new PalabraAI("client-id", "client-secret", "https://custom.api.com");
        assertEquals("client-id", testClient.getClientId());
        assertEquals("client-secret", testClient.getClientSecret());
        assertEquals("https://custom.api.com", testClient.getApiEndpoint());
    }

    @Test
    void testConstructorWithNullClientId() {
        assertThrows(IllegalArgumentException.class, () -> 
            new PalabraAI(null, "client-secret"));
    }

    @Test
    void testConstructorWithEmptyClientId() {
        assertThrows(IllegalArgumentException.class, () -> 
            new PalabraAI("", "client-secret"));
    }

    @Test
    void testConstructorWithNullClientSecret() {
        assertThrows(IllegalArgumentException.class, () -> 
            new PalabraAI("client-id", null));
    }

    @Test
    void testConstructorWithEmptyClientSecret() {
        assertThrows(IllegalArgumentException.class, () -> 
            new PalabraAI("client-id", ""));
    }

    @Test
    void testConstructorWithNullApiEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> 
            new PalabraAI("client-id", "client-secret", null));
    }

    @Test
    void testConstructorWithEmptyApiEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> 
            new PalabraAI("client-id", "client-secret", ""));
    }

    @Test
    void testRunWithNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> client.run(null));
    }

    @Test
    void testRunAsyncWithNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> client.runAsync(null));
    }

    @Test
    void testRunWithValidConfigButNoSession() {
        // Test that the method attempts to create a session but fails gracefully
        // The actual REST call will fail since we're using test credentials
        assertThrows(RuntimeException.class, () -> client.run(validConfig));
    }

    @Test
    void testRunAsyncWithValidConfigButNoSession() {
        CompletableFuture<Void> future = client.runAsync(validConfig);
        assertThrows(ExecutionException.class, () -> future.get());
    }

    @Test
    void testValidateConfigWithMissingSourceLanguage() {
        assertThrows(IllegalStateException.class, () -> {
            Config.builder()
                    .targetLang(Language.ES_MX)
                    .reader(new FileReader("src/test/resources/audio/en.wav"))
                    .writer(new FileWriter("test-output.wav"))
                    .build();
        });
    }

    @Test
    void testValidateConfigWithMissingTargetLanguage() {
        assertThrows(IllegalStateException.class, () -> {
            Config.builder()
                    .sourceLang(Language.EN_US)
                    .reader(new FileReader("src/test/resources/audio/en.wav"))
                    .writer(new FileWriter("test-output.wav"))
                    .build();
        });
    }

    @Test
    void testValidateConfigWithMissingReader() {
        assertThrows(IllegalStateException.class, () -> {
            Config.builder()
                    .sourceLang(Language.EN_US)
                    .targetLang(Language.ES_MX)
                    .writer(new FileWriter("test-output.wav"))
                    .build();
        });
    }

    @Test
    void testValidateConfigWithMissingWriter() {
        assertThrows(IllegalStateException.class, () -> {
            Config.builder()
                    .sourceLang(Language.EN_US)
                    .targetLang(Language.ES_MX)
                    .reader(new FileReader("test-input.wav"))
                    .build();
        });
    }

    @Test
    void testRunWithMockedRESTClient() throws PalabraException {
        // Test successful session creation and validation
        try (MockedStatic<PalabraRESTClient> mockedStatic = mockStatic(PalabraRESTClient.class)) {
            PalabraRESTClient mockRestClient = mock(PalabraRESTClient.class);
            SessionCredentials mockCredentials = new SessionCredentials();
            mockCredentials.setPublisher("test-publisher-token");
            mockCredentials.setSubscriber("test-subscriber-token");
            mockCredentials.setControlUrl("wss://test.control.url");
            mockCredentials.setStreamUrl("wss://test.stream.url");
            
            when(mockRestClient.createSession()).thenReturn(mockCredentials);
            
            // Create a new client that would use the mocked REST client
            // Note: This test demonstrates the structure but would require refactoring
            // the PalabraAI class to make it more testable with dependency injection
            
            // For now, we verify that the validation works correctly
            assertDoesNotThrow(() -> {
                // This will still fail at the REST call, but validation should pass
                try {
                    client.run(validConfig);
                } catch (RuntimeException e) {
                    // Expected due to real REST call failure
                    assertTrue(e.getMessage().contains("Translation session failed"));
                }
            });
        }
    }

    @Test
    void testClientCredentialsAreImmutable() {
        // Verify that the client ID and secret cannot be modified after construction
        String originalClientId = client.getClientId();
        String originalClientSecret = client.getClientSecret();
        String originalApiEndpoint = client.getApiEndpoint();
        
        assertEquals("test-client-id", originalClientId);
        assertEquals("test-client-secret", originalClientSecret);
        assertEquals("https://api.palabra.ai", originalApiEndpoint);
        
        // These should remain the same throughout the client's lifetime
        assertEquals(originalClientId, client.getClientId());
        assertEquals(originalClientSecret, client.getClientSecret());
        assertEquals(originalApiEndpoint, client.getApiEndpoint());
    }

    @Test
    void testTrimsWhitespaceInConstructor() {
        PalabraAI testClient = new PalabraAI("  client-id  ", "  client-secret  ", "  https://api.test.com  ");
        assertEquals("client-id", testClient.getClientId());
        assertEquals("client-secret", testClient.getClientSecret());
        assertEquals("https://api.test.com", testClient.getApiEndpoint());
    }
    
    @Test
    void testRunWithAdvancedConfig() {
        // Test that PalabraAI can work with AdvancedConfig
        Config advancedConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        // This will fail at the REST call, but should validate and convert the config properly
        assertThrows(RuntimeException.class, () -> client.run(advancedConfig));
    }
    
    @Test
    void testRunAsyncWithAdvancedConfig() {
        Config advancedConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        CompletableFuture<Void> future = client.runAsync(advancedConfig);
        assertThrows(ExecutionException.class, () -> future.get());
    }
    
    @Test
    void testAdvancedConfigWithMultipleTargets() {
        Config multiTargetConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output_es.wav"))
            .addTarget(Language.FR, new FileWriter("output_fr.wav"))
            .addTarget(Language.DE, new FileWriter("output_de.wav"))
            .build();
        
        assertTrue(multiTargetConfig.isAdvanced());
        assertEquals(3, multiTargetConfig.getTargets().size());
        
        // Should handle multi-target configuration validation
        assertThrows(RuntimeException.class, () -> client.run(multiTargetConfig));
    }
    
    @Test
    void testAdvancedConfigWithStreamSettings() {
        InputStreamConfig inputStream = InputStreamConfig.websocket(
            AudioFormat.builder()
                .format("pcm_s16le")
                .sampleRate(48000)
                .channels(1)
                .build()
        );
        
        OutputStreamConfig outputStream = OutputStreamConfig.websocket("pcm_s16le");
        
        Config streamConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .inputStream(inputStream)
            .outputStream(outputStream)
            .build();
        
        assertTrue(streamConfig.isAdvanced());
        assertNotNull(streamConfig.getAdvancedConfig().getInputStream());
        assertNotNull(streamConfig.getAdvancedConfig().getOutputStream());
        
        // Should handle stream configuration properly
        assertThrows(RuntimeException.class, () -> client.run(streamConfig));
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
        
        Config transcriptionConfig = Config.builder()
            .advanced()
            .source(source)
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        assertTrue(transcriptionConfig.isAdvanced());
        assertNotNull(transcriptionConfig.getAdvancedConfig().getSource().getTranscription());
        assertEquals("whisper-large", transcriptionConfig.getAdvancedConfig().getSource().getTranscription().getAsrModel());
        
        // Should handle transcription configuration properly
        assertThrows(RuntimeException.class, () -> client.run(transcriptionConfig));
    }
    
    @Test
    void testAdvancedConfigWithSpeechGeneration() {
        SpeechGenerationConfig speechGen = SpeechGenerationConfig.builder()
            .ttsModel("tacotron2")
            .voiceId("voice-001")
            .voiceCloning(true)
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
        
        Config speechConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(target)
            .build();
        
        assertTrue(speechConfig.isAdvanced());
        TargetLangConfig targetConfig = speechConfig.getAdvancedConfig().getTargets().get(0);
        assertNotNull(targetConfig.getTranslation().getSpeechGeneration());
        assertEquals("tacotron2", targetConfig.getTranslation().getSpeechGeneration().getTtsModel());
        assertEquals("voice-001", targetConfig.getTranslation().getSpeechGeneration().getVoiceId());
        assertTrue(targetConfig.getTranslation().getSpeechGeneration().isVoiceCloning());
        
        // Should handle speech generation configuration properly
        assertThrows(RuntimeException.class, () -> client.run(speechConfig));
    }
    
    @Test
    void testBackwardCompatibilityInPalabraAI() {
        // Test that PalabraAI continues to work with simple configurations
        Config simpleConfig = Config.builder()
            .sourceLang(Language.EN_US)
            .targetLang(Language.ES_MX)
            .reader(new FileReader("input.wav"))
            .writer(new FileWriter("output.wav"))
            .build();
        
        assertFalse(simpleConfig.isAdvanced());
        
        // PalabraAI should internally convert this to AdvancedConfig
        assertThrows(RuntimeException.class, () -> client.run(simpleConfig));
        
        // Verify that the config internally has AdvancedConfig
        assertNotNull(simpleConfig.getAdvancedConfig());
        assertEquals(1, simpleConfig.getTargets().size());
    }
    
    @Test
    void testProcessAsyncWithAdvancedConfig() {
        Config advancedConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        CompletableFuture<Void> future = client.processAsync(advancedConfig);
        assertThrows(ExecutionException.class, () -> future.get());
    }
    
    @Test
    void testProcessAsyncCancellableWithAdvancedConfig() {
        Config advancedConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        CompletableFuture<Void> future = client.processAsyncCancellable(advancedConfig);
        
        // Test that the future can be cancelled
        assertTrue(future.cancel(true));
        assertTrue(future.isCancelled());
    }
    
    @Test
    void testAdvancedConfigValidationInPalabraAI() {
        // Test that PalabraAI properly validates AdvancedConfig
        
        // Missing source should throw exception during build
        assertThrows(IllegalStateException.class, () -> {
            Config.builder()
                .advanced()
                .addTarget(Language.ES_MX, new FileWriter("output.wav"))
                .build();
        });
        
        // Missing targets should throw exception during build
        assertThrows(IllegalStateException.class, () -> {
            Config.builder()
                .advanced()
                .source(Language.EN_US, new FileReader("input.wav"))
                .build();
        });
    }
}
