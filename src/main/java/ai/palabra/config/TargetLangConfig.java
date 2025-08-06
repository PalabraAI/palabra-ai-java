package ai.palabra.config;

import ai.palabra.Language;
import ai.palabra.Writer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Configuration for target language including translation settings.
 */
@JsonDeserialize(builder = TargetLangConfig.Builder.class)
public class TargetLangConfig {
    private final Language language;
    private final TranslationConfig translation;
    private final Writer writer;
    
    private TargetLangConfig(Builder builder) {
        this.language = builder.language;
        this.translation = builder.translation;
        this.writer = builder.writer;
    }
    
    @JsonProperty("lang")
    public Language getLanguage() {
        return language;
    }
    
    @JsonProperty("translation")
    public TranslationConfig getTranslation() {
        return translation;
    }
    
    @JsonIgnore
    public Writer getWriter() {
        return writer;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Language language;
        private TranslationConfig translation;
        private Writer writer;
        
        @JsonProperty("lang")
        public Builder language(Language language) {
            this.language = language;
            return this;
        }
        
        public Builder language(String languageCode) {
            this.language = Language.fromCode(languageCode);
            return this;
        }
        
        @JsonProperty("translation")
        public Builder translation(TranslationConfig translation) {
            this.translation = translation;
            return this;
        }
        
        public Builder writer(Writer writer) {
            this.writer = writer;
            return this;
        }
        
        public TargetLangConfig build() {
            if (language == null) {
                throw new IllegalStateException("Language is required");
            }
            
            // Build translation with target language if not already set
            if (translation == null) {
                translation = TranslationConfig.builder()
                    .targetLanguage(language.getCode())
                    .build();
            } else if (translation.getTargetLanguage() == null) {
                translation = TranslationConfig.builder()
                    .targetLanguage(language.getCode())
                    .build();
            }
            
            return new TargetLangConfig(this);
        }
    }
}