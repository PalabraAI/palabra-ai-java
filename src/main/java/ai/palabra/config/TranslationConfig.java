package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Translation configuration for language translation.
 */
@JsonDeserialize(builder = TranslationConfig.Builder.class)
public class TranslationConfig {
    private final String targetLanguage;
    private final List<String> allowedSourceLanguages;
    private final String translationModel;
    private final boolean allowTranslationGlossaries;
    private final String style;
    private final boolean translatePartialTranscriptions;
    private final SpeechGenerationConfig speechGeneration;
    
    private TranslationConfig(Builder builder) {
        this.targetLanguage = builder.targetLanguage;
        this.allowedSourceLanguages = new ArrayList<>(builder.allowedSourceLanguages);
        this.translationModel = builder.translationModel;
        this.allowTranslationGlossaries = builder.allowTranslationGlossaries;
        this.style = builder.style;
        this.translatePartialTranscriptions = builder.translatePartialTranscriptions;
        this.speechGeneration = builder.speechGeneration;
    }
    
    @JsonProperty("target_language")
    public String getTargetLanguage() {
        return targetLanguage;
    }
    
    @JsonProperty("allowed_source_languages")
    public List<String> getAllowedSourceLanguages() {
        return new ArrayList<>(allowedSourceLanguages);
    }
    
    @JsonProperty("translation_model")
    public String getTranslationModel() {
        return translationModel;
    }
    
    @JsonProperty("allow_translation_glossaries")
    public boolean isAllowTranslationGlossaries() {
        return allowTranslationGlossaries;
    }
    
    @JsonProperty("style")
    public String getStyle() {
        return style;
    }
    
    @JsonProperty("translate_partial_transcriptions")
    public boolean isTranslatePartialTranscriptions() {
        return translatePartialTranscriptions;
    }
    
    @JsonProperty("speech_generation")
    public SpeechGenerationConfig getSpeechGeneration() {
        return speechGeneration;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String targetLanguage;
        private List<String> allowedSourceLanguages = new ArrayList<>();
        private String translationModel = ConfigConstants.AUTO_MODEL;
        private boolean allowTranslationGlossaries = true;
        private String style = null;
        private boolean translatePartialTranscriptions = false;
        private SpeechGenerationConfig speechGeneration = SpeechGenerationConfig.builder().build();
        
        @JsonProperty("target_language")
        public Builder targetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
            return this;
        }
        
        @JsonProperty("allowed_source_languages")
        public Builder allowedSourceLanguages(List<String> allowedSourceLanguages) {
            this.allowedSourceLanguages = new ArrayList<>(allowedSourceLanguages);
            return this;
        }
        
        public Builder addAllowedSourceLanguage(String language) {
            this.allowedSourceLanguages.add(language);
            return this;
        }
        
        @JsonProperty("translation_model")
        public Builder translationModel(String translationModel) {
            this.translationModel = translationModel;
            return this;
        }
        
        @JsonProperty("allow_translation_glossaries")
        public Builder allowTranslationGlossaries(boolean allowTranslationGlossaries) {
            this.allowTranslationGlossaries = allowTranslationGlossaries;
            return this;
        }
        
        public Builder style(String style) {
            this.style = style;
            return this;
        }
        
        @JsonProperty("translate_partial_transcriptions")
        public Builder translatePartialTranscriptions(boolean translatePartialTranscriptions) {
            this.translatePartialTranscriptions = translatePartialTranscriptions;
            return this;
        }
        
        @JsonProperty("speech_generation")
        public Builder speechGeneration(SpeechGenerationConfig speechGeneration) {
            this.speechGeneration = speechGeneration;
            return this;
        }
        
        public TranslationConfig build() {
            return new TranslationConfig(this);
        }
    }
}