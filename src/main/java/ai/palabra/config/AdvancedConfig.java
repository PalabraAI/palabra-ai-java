package ai.palabra.config;

import ai.palabra.Language;
import ai.palabra.Reader;
import ai.palabra.Writer;
import ai.palabra.base.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Advanced configuration class for Palabra AI translation sessions.
 * Supports all WebSocket configuration options and JSON loading.
 * 
 * This is the new configuration class that extends the simple Config class
 * with full support for all Palabra AI settings.
 * 
 * Example usage:
 * <pre>
 * AdvancedConfig config = AdvancedConfig.builder()
 *     .source(SourceLangConfig.builder()
 *         .language(Language.EN_US)
 *         .reader(new FileReader("input.wav"))
 *         .transcription(TranscriptionConfig.builder()
 *             .segmentConfirmationSilenceThreshold(0.8f)
 *             .build())
 *         .build())
 *     .addTarget(TargetLangConfig.builder()
 *         .language(Language.ES_MX)
 *         .writer(new FileWriter("output.wav"))
 *         .translation(TranslationConfig.builder()
 *             .translatePartialTranscriptions(true)
 *             .build())
 *         .build())
 *     .build();
 * </pre>
 */
@JsonDeserialize(builder = AdvancedConfig.Builder.class)
public class AdvancedConfig {
    private final SourceLangConfig source;
    private final List<TargetLangConfig> targets;
    private final InputStreamConfig inputStream;
    private final OutputStreamConfig outputStream;
    private final TranslationQueueConfigs translationQueueConfigs;
    private final List<String> allowedMessageTypes;
    
    // Additional settings
    private final boolean silent;
    private final boolean debug;
    private final int timeout;
    
    private AdvancedConfig(Builder builder) {
        this.source = builder.source;
        this.targets = new ArrayList<>(builder.targets);
        this.inputStream = builder.inputStream;
        this.outputStream = builder.outputStream;
        this.translationQueueConfigs = builder.translationQueueConfigs;
        this.allowedMessageTypes = new ArrayList<>(builder.allowedMessageTypes);
        this.silent = builder.silent;
        this.debug = builder.debug;
        this.timeout = builder.timeout;
    }
    
    @JsonProperty("source")
    public SourceLangConfig getSource() {
        return source;
    }
    
    @JsonProperty("targets")
    public List<TargetLangConfig> getTargets() {
        return new ArrayList<>(targets);
    }
    
    @JsonProperty("input_stream")
    public InputStreamConfig getInputStream() {
        return inputStream;
    }
    
    @JsonProperty("output_stream")
    public OutputStreamConfig getOutputStream() {
        return outputStream;
    }
    
    @JsonProperty("translation_queue_configs")
    public TranslationQueueConfigs getTranslationQueueConfigs() {
        return translationQueueConfigs;
    }
    
    @JsonProperty("allowed_message_types")
    public List<String> getAllowedMessageTypes() {
        return new ArrayList<>(allowedMessageTypes);
    }
    
    @JsonIgnore
    public boolean isSilent() {
        return silent;
    }
    
    @JsonIgnore
    public boolean isDebug() {
        return debug;
    }
    
    @JsonIgnore
    public int getTimeout() {
        return timeout;
    }
    
    /**
     * Load configuration from JSON file.
     */
    public static AdvancedConfig fromFile(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(), AdvancedConfig.class);
    }
    
    /**
     * Load configuration from JSON file.
     */
    public static AdvancedConfig fromFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, AdvancedConfig.class);
    }
    
    /**
     * Load configuration from JSON string.
     */
    public static AdvancedConfig fromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, AdvancedConfig.class);
    }
    
    /**
     * Convert configuration to JSON string.
     */
    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
    
    /**
     * Save configuration to JSON file.
     */
    public void saveToFile(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private SourceLangConfig source;
        private List<TargetLangConfig> targets = new ArrayList<>();
        private InputStreamConfig inputStream = InputStreamConfig.websocket();
        private OutputStreamConfig outputStream = OutputStreamConfig.websocket();
        private TranslationQueueConfigs translationQueueConfigs = TranslationQueueConfigs.builder().build();
        private List<String> allowedMessageTypes = Arrays.asList(
            "partial_transcription",
            "validated_transcription", 
            "translation",
            "tts_started",
            "output_audio_data",
            "error"
        );
        private boolean silent = false;
        private boolean debug = false;
        private int timeout = 0;
        
        @JsonProperty("source")
        public Builder source(SourceLangConfig source) {
            this.source = source;
            return this;
        }
        
        /**
         * Convenience method to set source with language and reader.
         */
        public Builder source(Language language, Reader reader) {
            this.source = SourceLangConfig.builder()
                .language(language)
                .reader(reader)
                .build();
            return this;
        }
        
        @JsonProperty("targets")
        public Builder targets(List<TargetLangConfig> targets) {
            this.targets = new ArrayList<>(targets);
            return this;
        }
        
        public Builder addTarget(TargetLangConfig target) {
            this.targets.add(target);
            return this;
        }
        
        /**
         * Convenience method to add target with language and writer.
         */
        public Builder addTarget(Language language, Writer writer) {
            this.targets.add(TargetLangConfig.builder()
                .language(language)
                .writer(writer)
                .build());
            return this;
        }
        
        @JsonProperty("input_stream")
        public Builder inputStream(InputStreamConfig inputStream) {
            this.inputStream = inputStream;
            return this;
        }
        
        @JsonProperty("output_stream")
        public Builder outputStream(OutputStreamConfig outputStream) {
            this.outputStream = outputStream;
            return this;
        }
        
        @JsonProperty("translation_queue_configs")
        public Builder translationQueueConfigs(TranslationQueueConfigs translationQueueConfigs) {
            this.translationQueueConfigs = translationQueueConfigs;
            return this;
        }
        
        @JsonProperty("allowed_message_types")
        public Builder allowedMessageTypes(List<String> allowedMessageTypes) {
            this.allowedMessageTypes = new ArrayList<>(allowedMessageTypes);
            return this;
        }
        
        public Builder silent(boolean silent) {
            this.silent = silent;
            return this;
        }
        
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }
        
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public AdvancedConfig build() {
            if (source == null) {
                throw new IllegalStateException("Source configuration is required");
            }
            if (targets.isEmpty()) {
                throw new IllegalStateException("At least one target configuration is required");
            }
            
            return new AdvancedConfig(this);
        }
    }
}