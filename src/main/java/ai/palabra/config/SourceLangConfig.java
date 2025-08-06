package ai.palabra.config;

import ai.palabra.Language;
import ai.palabra.Reader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Configuration for source language including transcription settings.
 */
@JsonDeserialize(builder = SourceLangConfig.Builder.class)
public class SourceLangConfig {
    private final Language language;
    private final TranscriptionConfig transcription;
    private final Reader reader;
    
    private SourceLangConfig(Builder builder) {
        this.language = builder.language;
        this.transcription = builder.transcription;
        this.reader = builder.reader;
    }
    
    @JsonProperty("lang")
    public Language getLanguage() {
        return language;
    }
    
    @JsonProperty("transcription")
    public TranscriptionConfig getTranscription() {
        return transcription;
    }
    
    @JsonIgnore
    public Reader getReader() {
        return reader;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Language language;
        private TranscriptionConfig transcription;
        private Reader reader;
        
        @JsonProperty("lang")
        public Builder language(Language language) {
            this.language = language;
            return this;
        }
        
        public Builder language(String languageCode) {
            this.language = Language.fromCode(languageCode);
            return this;
        }
        
        @JsonProperty("transcription")
        public Builder transcription(TranscriptionConfig transcription) {
            this.transcription = transcription;
            return this;
        }
        
        public Builder reader(Reader reader) {
            this.reader = reader;
            return this;
        }
        
        public SourceLangConfig build() {
            if (language == null) {
                throw new IllegalStateException("Language is required");
            }
            
            // Build transcription with source language if not already set
            if (transcription == null) {
                transcription = TranscriptionConfig.builder()
                    .sourceLanguage(language.getCode())
                    .build();
            } else if (transcription.getSourceLanguage() == null) {
                transcription = TranscriptionConfig.builder()
                    .sourceLanguage(language.getCode())
                    .build();
            }
            
            return new SourceLangConfig(this);
        }
    }
}