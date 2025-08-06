package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for translation queue configurations.
 * Supports global config and per-language configs.
 */
@JsonDeserialize(builder = TranslationQueueConfigs.Builder.class)
public class TranslationQueueConfigs {
    private final QueueConfig global;
    private final Map<String, QueueConfig> perLanguage;
    
    private TranslationQueueConfigs(Builder builder) {
        this.global = builder.global;
        this.perLanguage = new HashMap<>(builder.perLanguage);
    }
    
    @JsonProperty("global")
    @JsonAlias("global_")
    public QueueConfig getGlobal() {
        return global;
    }
    
    @JsonProperty("per_language")
    public Map<String, QueueConfig> getPerLanguage() {
        return new HashMap<>(perLanguage);
    }
    
    /**
     * Get queue config for a specific language, falling back to global if not found.
     */
    public QueueConfig getForLanguage(String language) {
        return perLanguage.getOrDefault(language, global);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private QueueConfig global = QueueConfig.builder().build();
        private Map<String, QueueConfig> perLanguage = new HashMap<>();
        
        @JsonProperty("global")
        @JsonAlias("global_")
        public Builder global(QueueConfig global) {
            this.global = global;
            return this;
        }
        
        @JsonProperty("per_language")
        public Builder perLanguage(Map<String, QueueConfig> perLanguage) {
            this.perLanguage = new HashMap<>(perLanguage);
            return this;
        }
        
        public Builder addLanguageConfig(String language, QueueConfig config) {
            this.perLanguage.put(language, config);
            return this;
        }
        
        public TranslationQueueConfigs build() {
            return new TranslationQueueConfigs(this);
        }
    }
}