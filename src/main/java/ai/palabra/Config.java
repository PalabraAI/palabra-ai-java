package ai.palabra;

import ai.palabra.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for Palabra AI translation sessions.
 * Uses builder pattern for fluent configuration matching the Python library structure.
 * 
 * This class maintains backward compatibility while providing access to advanced features
 * through the AdvancedConfig class.
 * 
 * Example usage:
 * <pre>
 * // Simple configuration (backward compatible)
 * Config config = Config.builder()
 *     .sourceLang(Language.EN_US)
 *     .targetLang(Language.ES_MX)
 *     .reader(new FileReader("input.wav"))
 *     .writer(new FileWriter("output.wav"))
 *     .build();
 * 
 * // Advanced configuration
 * Config advancedConfig = Config.builder()
 *     .advanced()
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
 *         .build())
 *     .build();
 * </pre>
 */
public class Config {
    private final Language sourceLang;
    private final Language targetLang;
    private final Reader reader;
    private final Writer writer;
    
    // Advanced configuration fields
    private final AdvancedConfig advancedConfig;
    private final boolean isAdvanced;
    
    private Config(Builder builder) {
        if (builder.isAdvanced) {
            // Build from advanced configuration
            this.advancedConfig = builder.advancedBuilder.build();
            this.isAdvanced = true;
            
            // Extract simple fields for backward compatibility
            SourceLangConfig source = advancedConfig.getSource();
            List<TargetLangConfig> targets = advancedConfig.getTargets();
            
            this.sourceLang = source.getLanguage();
            this.reader = source.getReader();
            
            if (!targets.isEmpty()) {
                TargetLangConfig firstTarget = targets.get(0);
                this.targetLang = firstTarget.getLanguage();
                this.writer = firstTarget.getWriter();
            } else {
                this.targetLang = null;
                this.writer = null;
            }
        } else {
            // Simple configuration
            this.sourceLang = builder.sourceLang;
            this.targetLang = builder.targetLang;
            this.reader = builder.reader;
            this.writer = builder.writer;
            this.isAdvanced = false;
            
            // Build advanced config from simple fields
            this.advancedConfig = AdvancedConfig.builder()
                .source(SourceLangConfig.builder()
                    .language(sourceLang)
                    .reader(reader)
                    .build())
                .addTarget(TargetLangConfig.builder()
                    .language(targetLang)
                    .writer(writer)
                    .build())
                .build();
        }
    }
    
    /**
     * Creates a new builder for Config.
     * 
     * @return A new Config.Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the source language for translation.
     * 
     * @return The source language
     */
    public Language getSourceLang() {
        return sourceLang;
    }
    
    /**
     * Gets the target language for translation.
     * 
     * @return The target language
     */
    public Language getTargetLang() {
        return targetLang;
    }
    
    /**
     * Gets the audio input reader.
     * 
     * @return The reader instance
     */
    public Reader getReader() {
        return reader;
    }
    
    /**
     * Gets the audio output writer.
     * 
     * @return The writer instance
     */
    public Writer getWriter() {
        return writer;
    }
    
    /**
     * Gets the advanced configuration.
     * This provides access to all advanced settings.
     * 
     * @return The advanced configuration
     */
    public AdvancedConfig getAdvancedConfig() {
        return advancedConfig;
    }
    
    /**
     * Checks if this configuration was built with advanced settings.
     * 
     * @return true if advanced configuration was used
     */
    public boolean isAdvanced() {
        return isAdvanced;
    }
    
    /**
     * Gets all target language configurations.
     * 
     * @return List of target configurations
     */
    public List<TargetLangConfig> getTargets() {
        return advancedConfig.getTargets();
    }
    
    /**
     * Gets the input stream configuration.
     * 
     * @return Input stream configuration
     */
    public InputStreamConfig getInputStream() {
        return advancedConfig.getInputStream();
    }
    
    /**
     * Gets the output stream configuration.
     * 
     * @return Output stream configuration
     */
    public OutputStreamConfig getOutputStream() {
        return advancedConfig.getOutputStream();
    }
    
    /**
     * Load configuration from JSON file.
     * 
     * @param path Path to JSON configuration file
     * @return Config instance
     * @throws IOException if file cannot be read or parsed
     */
    public static Config fromFile(Path path) throws IOException {
        AdvancedConfig advanced = AdvancedConfig.fromFile(path);
        return fromAdvancedConfig(advanced);
    }
    
    /**
     * Load configuration from JSON file.
     * 
     * @param file JSON configuration file
     * @return Config instance
     * @throws IOException if file cannot be read or parsed
     */
    public static Config fromFile(File file) throws IOException {
        AdvancedConfig advanced = AdvancedConfig.fromFile(file);
        return fromAdvancedConfig(advanced);
    }
    
    /**
     * Load configuration from JSON string.
     * 
     * @param json JSON configuration string
     * @return Config instance
     * @throws IOException if JSON cannot be parsed
     */
    public static Config fromJson(String json) throws IOException {
        AdvancedConfig advanced = AdvancedConfig.fromJson(json);
        return fromAdvancedConfig(advanced);
    }
    
    /**
     * Create Config from AdvancedConfig.
     */
    private static Config fromAdvancedConfig(AdvancedConfig advanced) {
        Builder builder = new Builder();
        builder.isAdvanced = true;
        builder.advancedBuilder = AdvancedConfig.builder()
            .source(advanced.getSource())
            .targets(advanced.getTargets())
            .inputStream(advanced.getInputStream())
            .outputStream(advanced.getOutputStream())
            .translationQueueConfigs(advanced.getTranslationQueueConfigs())
            .allowedMessageTypes(advanced.getAllowedMessageTypes())
            .silent(advanced.isSilent())
            .debug(advanced.isDebug())
            .timeout(advanced.getTimeout());
        return builder.build();
    }
    
    /**
     * Convert configuration to JSON string.
     * 
     * @return JSON representation
     * @throws IOException if serialization fails
     */
    public String toJson() throws IOException {
        return advancedConfig.toJson();
    }
    
    /**
     * Save configuration to JSON file.
     * 
     * @param path Target file path
     * @throws IOException if file cannot be written
     */
    public void saveToFile(Path path) throws IOException {
        advancedConfig.saveToFile(path);
    }
    
    /**
     * Builder class for Config using fluent interface pattern.
     * Supports both simple and advanced configuration modes.
     */
    public static class Builder {
        private Language sourceLang;
        private Language targetLang;
        private Reader reader;
        private Writer writer;
        
        // Advanced mode fields
        private boolean isAdvanced = false;
        private AdvancedConfig.Builder advancedBuilder;
        
        private Builder() {}
        
        /**
         * Sets the source language for translation.
         * 
         * @param sourceLang The source language
         * @return This builder instance
         */
        public Builder sourceLang(Language sourceLang) {
            this.sourceLang = sourceLang;
            return this;
        }
        
        /**
         * Sets the target language for translation.
         * 
         * @param targetLang The target language
         * @return This builder instance
         */
        public Builder targetLang(Language targetLang) {
            this.targetLang = targetLang;
            return this;
        }
        
        /**
         * Sets the audio input reader.
         * 
         * @param reader The reader instance
         * @return This builder instance
         */
        public Builder reader(Reader reader) {
            this.reader = reader;
            return this;
        }
        
        /**
         * Sets the audio output writer.
         * 
         * @param writer The writer instance
         * @return This builder instance
         */
        public Builder writer(Writer writer) {
            this.writer = writer;
            return this;
        }
        
        /**
         * Switch to advanced configuration mode.
         * This returns an AdvancedBuilder for setting advanced options.
         * 
         * @return Advanced configuration builder
         */
        public AdvancedBuilder advanced() {
            this.isAdvanced = true;
            this.advancedBuilder = AdvancedConfig.builder();
            return new AdvancedBuilder(this, advancedBuilder);
        }
        
        /**
         * Builds the Config instance with validation.
         * 
         * @return A new Config instance
         * @throws IllegalStateException if required fields are missing
         */
        public Config build() {
            if (!isAdvanced) {
                if (sourceLang == null) {
                    throw new IllegalStateException("Source language is required");
                }
                if (targetLang == null) {
                    throw new IllegalStateException("Target language is required");
                }
                if (reader == null) {
                    throw new IllegalStateException("Reader is required");
                }
                if (writer == null) {
                    throw new IllegalStateException("Writer is required");
                }
            }
            
            return new Config(this);
        }
    }
    
    /**
     * Advanced builder for complex configurations.
     * Provides access to all Palabra AI settings.
     */
    public static class AdvancedBuilder {
        private final Builder parent;
        private final AdvancedConfig.Builder advancedBuilder;
        
        private AdvancedBuilder(Builder parent, AdvancedConfig.Builder advancedBuilder) {
            this.parent = parent;
            this.advancedBuilder = advancedBuilder;
        }
        
        public AdvancedBuilder source(SourceLangConfig source) {
            advancedBuilder.source(source);
            return this;
        }
        
        public AdvancedBuilder source(Language language, Reader reader) {
            advancedBuilder.source(language, reader);
            return this;
        }
        
        public AdvancedBuilder targets(List<TargetLangConfig> targets) {
            advancedBuilder.targets(targets);
            return this;
        }
        
        public AdvancedBuilder addTarget(TargetLangConfig target) {
            advancedBuilder.addTarget(target);
            return this;
        }
        
        public AdvancedBuilder addTarget(Language language, Writer writer) {
            advancedBuilder.addTarget(language, writer);
            return this;
        }
        
        public AdvancedBuilder inputStream(InputStreamConfig inputStream) {
            advancedBuilder.inputStream(inputStream);
            return this;
        }
        
        public AdvancedBuilder outputStream(OutputStreamConfig outputStream) {
            advancedBuilder.outputStream(outputStream);
            return this;
        }
        
        public AdvancedBuilder translationQueueConfigs(TranslationQueueConfigs configs) {
            advancedBuilder.translationQueueConfigs(configs);
            return this;
        }
        
        public AdvancedBuilder allowedMessageTypes(List<String> types) {
            advancedBuilder.allowedMessageTypes(types);
            return this;
        }
        
        public AdvancedBuilder silent(boolean silent) {
            advancedBuilder.silent(silent);
            return this;
        }
        
        public AdvancedBuilder debug(boolean debug) {
            advancedBuilder.debug(debug);
            return this;
        }
        
        public AdvancedBuilder timeout(int timeout) {
            advancedBuilder.timeout(timeout);
            return this;
        }
        
        /**
         * Build the configuration.
         * 
         * @return Config instance with advanced settings
         */
        public Config build() {
            return parent.build();
        }
    }
}
