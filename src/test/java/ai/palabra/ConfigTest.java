package ai.palabra;

import ai.palabra.adapter.FileReader;
import ai.palabra.adapter.FileWriter;
import ai.palabra.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Tests for the Config class and builder pattern.
 */
class ConfigTest {
    
    @Test
    void testConfigBuilderSuccess() {
        Config config = Config.builder()
            .sourceLang(Language.EN_US)
            .targetLang(Language.ES_MX)
            .reader(new FileReader("input.wav"))
            .writer(new FileWriter("output.wav"))
            .build();
        
        assertEquals(Language.EN_US, config.getSourceLang());
        assertEquals(Language.ES_MX, config.getTargetLang());
        assertNotNull(config.getReader());
        assertNotNull(config.getWriter());
        assertTrue(config.getReader() instanceof FileReader);
        assertTrue(config.getWriter() instanceof FileWriter);
    }
    
    @Test
    void testConfigBuilderValidation() {
        // Test missing source language
        assertThrows(IllegalStateException.class, () ->
            Config.builder()
                .targetLang(Language.ES_MX)
                .reader(new FileReader("input.wav"))
                .writer(new FileWriter("output.wav"))
                .build());
        
        // Test missing target language
        assertThrows(IllegalStateException.class, () ->
            Config.builder()
                .sourceLang(Language.EN_US)
                .reader(new FileReader("input.wav"))
                .writer(new FileWriter("output.wav"))
                .build());
        
        // Test missing reader
        assertThrows(IllegalStateException.class, () ->
            Config.builder()
                .sourceLang(Language.EN_US)
                .targetLang(Language.ES_MX)
                .writer(new FileWriter("output.wav"))
                .build());
        
        // Test missing writer
        assertThrows(IllegalStateException.class, () ->
            Config.builder()
                .sourceLang(Language.EN_US)
                .targetLang(Language.ES_MX)
                .reader(new FileReader("input.wav"))
                .build());
    }
    
    @Test
    void testConfigBuilderFluentInterface() {
        Config.Builder builder = Config.builder();
        
        // Test that methods return the builder for chaining
        assertSame(builder, builder.sourceLang(Language.EN_US));
        assertSame(builder, builder.targetLang(Language.ES_MX));
        assertSame(builder, builder.reader(new FileReader("input.wav")));
        assertSame(builder, builder.writer(new FileWriter("output.wav")));
    }
    
    @Test
    void testAdvancedConfigIntegration() {
        // Test that Config can work with AdvancedConfig
        Config config = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        assertTrue(config.isAdvanced());
        assertEquals(Language.EN_US, config.getSourceLang());
        assertEquals(Language.ES_MX, config.getTargetLang());
        assertNotNull(config.getAdvancedConfig());
        assertNotNull(config.getTargets());
        assertEquals(1, config.getTargets().size());
    }
    
    @Test
    void testAdvancedConfigMultipleTargets() {
        // Test advanced config with multiple targets
        Config config = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output_es.wav"))
            .addTarget(Language.FR, new FileWriter("output_fr.wav"))
            .build();
        
        assertTrue(config.isAdvanced());
        assertEquals(2, config.getTargets().size());
        
        // Verify first target
        assertEquals(Language.ES_MX, config.getTargets().get(0).getLanguage());
        assertNotNull(config.getTargets().get(0).getWriter());
        
        // Verify second target  
        assertEquals(Language.FR, config.getTargets().get(1).getLanguage());
        assertNotNull(config.getTargets().get(1).getWriter());
    }
    
    @Test
    void testAdvancedConfigWithStreamSettings() {
        // Test advanced config with stream configurations
        InputStreamConfig inputStream = InputStreamConfig.websocket(
            AudioFormat.builder()
                .format("pcm_s16le")
                .sampleRate(48000)
                .channels(1)
                .build()
        );
        
        OutputStreamConfig outputStream = OutputStreamConfig.websocket("pcm_s16le");
        
        Config config = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .inputStream(inputStream)
            .outputStream(outputStream)
            .build();
        
        assertTrue(config.isAdvanced());
        AdvancedConfig advancedConfig = config.getAdvancedConfig();
        assertNotNull(advancedConfig.getInputStream());
        assertNotNull(advancedConfig.getOutputStream());
        assertEquals("ws", advancedConfig.getInputStream().getContentType());
        assertEquals("ws", advancedConfig.getOutputStream().getContentType());
    }
    
    @Test
    void testBackwardCompatibility() {
        // Test that simple config still works after advanced config changes
        Config simpleConfig = Config.builder()
            .sourceLang(Language.EN_US)
            .targetLang(Language.ES_MX)
            .reader(new FileReader("input.wav"))
            .writer(new FileWriter("output.wav"))
            .build();
        
        assertFalse(simpleConfig.isAdvanced());
        assertEquals(Language.EN_US, simpleConfig.getSourceLang());
        assertEquals(Language.ES_MX, simpleConfig.getTargetLang());
        
        // But it should still have an AdvancedConfig internally
        assertNotNull(simpleConfig.getAdvancedConfig());
        assertEquals(1, simpleConfig.getTargets().size());
    }
    
    @Test
    void testJsonLoadingSaving(@TempDir Path tempDir) throws IOException {
        // Test JSON serialization and deserialization
        Path configFile = tempDir.resolve("test-config.json");
        
        // Create a config with advanced settings
        Config originalConfig = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        // Save to JSON
        originalConfig.saveToFile(configFile);
        assertTrue(configFile.toFile().exists());
        
        // Load from JSON
        Config loadedConfig = Config.fromFile(configFile);
        assertTrue(loadedConfig.isAdvanced());
        assertEquals(Language.EN_US, loadedConfig.getSourceLang());
        assertEquals(Language.ES_MX, loadedConfig.getTargetLang());
        assertEquals(1, loadedConfig.getTargets().size());
    }
    
    @Test
    void testJsonString() throws IOException {
        // Test JSON string serialization
        Config config = Config.builder()
            .advanced()
            .source(Language.EN_US, new FileReader("input.wav"))
            .addTarget(Language.ES_MX, new FileWriter("output.wav"))
            .build();
        
        String json = config.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("source"));
        assertTrue(json.contains("targets"));
        
        // Test loading from JSON string
        Config loadedConfig = Config.fromJson(json);
        assertTrue(loadedConfig.isAdvanced());
        assertEquals(Language.EN_US, loadedConfig.getSourceLang());
        assertEquals(Language.ES_MX, loadedConfig.getTargetLang());
    }
    
    @Test
    void testAdvancedConfigValidation() {
        // Test that advanced config validation works
        assertThrows(IllegalStateException.class, () ->
            Config.builder()
                .advanced()
                .addTarget(Language.ES_MX, new FileWriter("output.wav"))
                .build()); // Missing source
        
        assertThrows(IllegalStateException.class, () ->
            Config.builder()
                .advanced()
                .source(Language.EN_US, new FileReader("input.wav"))
                .build()); // Missing targets
    }
}
