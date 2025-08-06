package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Transcription configuration for speech-to-text processing.
 */
@JsonDeserialize(builder = TranscriptionConfig.Builder.class)
public class TranscriptionConfig {
    private final String sourceLanguage;
    private final List<String> detectableLanguages;
    private final String asrModel;
    private final String denoise;
    private final boolean allowHotwordsGlossaries;
    private final boolean suppressNumeralTokens;
    private final boolean diarizeSpeakers;
    private final String priority;
    private final float minAlignmentScore;
    private final float maxAlignmentCer;
    private final float segmentConfirmationSilenceThreshold;
    private final boolean onlyConfirmBySilence;
    private final boolean batchedInference;
    private final boolean forceDetectLanguage;
    private final boolean calculateVoiceLoudness;
    private final SentenceSplitterConfig sentenceSplitter;
    
    private TranscriptionConfig(Builder builder) {
        this.sourceLanguage = builder.sourceLanguage;
        this.detectableLanguages = new ArrayList<>(builder.detectableLanguages);
        this.asrModel = builder.asrModel;
        this.denoise = builder.denoise;
        this.allowHotwordsGlossaries = builder.allowHotwordsGlossaries;
        this.suppressNumeralTokens = builder.suppressNumeralTokens;
        this.diarizeSpeakers = builder.diarizeSpeakers;
        this.priority = builder.priority;
        this.minAlignmentScore = builder.minAlignmentScore;
        this.maxAlignmentCer = builder.maxAlignmentCer;
        this.segmentConfirmationSilenceThreshold = builder.segmentConfirmationSilenceThreshold;
        this.onlyConfirmBySilence = builder.onlyConfirmBySilence;
        this.batchedInference = builder.batchedInference;
        this.forceDetectLanguage = builder.forceDetectLanguage;
        this.calculateVoiceLoudness = builder.calculateVoiceLoudness;
        this.sentenceSplitter = builder.sentenceSplitter;
    }
    
    @JsonProperty("source_language")
    public String getSourceLanguage() {
        return sourceLanguage;
    }
    
    @JsonProperty("detectable_languages")
    public List<String> getDetectableLanguages() {
        return new ArrayList<>(detectableLanguages);
    }
    
    @JsonProperty("asr_model")
    public String getAsrModel() {
        return asrModel;
    }
    
    @JsonProperty("denoise")
    public String getDenoise() {
        return denoise;
    }
    
    @JsonProperty("allow_hotwords_glossaries")
    public boolean isAllowHotwordsGlossaries() {
        return allowHotwordsGlossaries;
    }
    
    @JsonProperty("suppress_numeral_tokens")
    public boolean isSuppressNumeralTokens() {
        return suppressNumeralTokens;
    }
    
    @JsonProperty("diarize_speakers")
    public boolean isDiarizeSpeakers() {
        return diarizeSpeakers;
    }
    
    @JsonProperty("priority")
    public String getPriority() {
        return priority;
    }
    
    @JsonProperty("min_alignment_score")
    public float getMinAlignmentScore() {
        return minAlignmentScore;
    }
    
    @JsonProperty("max_alignment_cer")
    public float getMaxAlignmentCer() {
        return maxAlignmentCer;
    }
    
    @JsonProperty("segment_confirmation_silence_threshold")
    public float getSegmentConfirmationSilenceThreshold() {
        return segmentConfirmationSilenceThreshold;
    }
    
    @JsonProperty("only_confirm_by_silence")
    public boolean isOnlyConfirmBySilence() {
        return onlyConfirmBySilence;
    }
    
    @JsonProperty("batched_inference")
    public boolean isBatchedInference() {
        return batchedInference;
    }
    
    @JsonProperty("force_detect_language")
    public boolean isForceDetectLanguage() {
        return forceDetectLanguage;
    }
    
    @JsonProperty("calculate_voice_loudness")
    public boolean isCalculateVoiceLoudness() {
        return calculateVoiceLoudness;
    }
    
    @JsonProperty("sentence_splitter")
    public SentenceSplitterConfig getSentenceSplitter() {
        return sentenceSplitter;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String sourceLanguage;
        private List<String> detectableLanguages = new ArrayList<>();
        private String asrModel = ConfigConstants.AUTO_MODEL;
        private String denoise = ConfigConstants.DENOISE_NONE;
        private boolean allowHotwordsGlossaries = true;
        private boolean suppressNumeralTokens = false;
        private boolean diarizeSpeakers = false;
        private String priority = ConfigConstants.PRIORITY_NORMAL;
        private float minAlignmentScore = ConfigConstants.MIN_ALIGNMENT_SCORE_DEFAULT;
        private float maxAlignmentCer = ConfigConstants.MAX_ALIGNMENT_CER_DEFAULT;
        private float segmentConfirmationSilenceThreshold = ConfigConstants.SEGMENT_CONFIRMATION_SILENCE_THRESHOLD_DEFAULT;
        private boolean onlyConfirmBySilence = false;
        private boolean batchedInference = false;
        private boolean forceDetectLanguage = false;
        private boolean calculateVoiceLoudness = false;
        private SentenceSplitterConfig sentenceSplitter = SentenceSplitterConfig.builder().build();
        
        @JsonProperty("source_language")
        public Builder sourceLanguage(String sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
            return this;
        }
        
        @JsonProperty("detectable_languages")
        public Builder detectableLanguages(List<String> detectableLanguages) {
            this.detectableLanguages = new ArrayList<>(detectableLanguages);
            return this;
        }
        
        public Builder addDetectableLanguage(String language) {
            this.detectableLanguages.add(language);
            return this;
        }
        
        @JsonProperty("asr_model")
        public Builder asrModel(String asrModel) {
            this.asrModel = asrModel;
            return this;
        }
        
        public Builder denoise(String denoise) {
            this.denoise = denoise;
            return this;
        }
        
        @JsonProperty("allow_hotwords_glossaries")
        public Builder allowHotwordsGlossaries(boolean allowHotwordsGlossaries) {
            this.allowHotwordsGlossaries = allowHotwordsGlossaries;
            return this;
        }
        
        @JsonProperty("suppress_numeral_tokens")
        public Builder suppressNumeralTokens(boolean suppressNumeralTokens) {
            this.suppressNumeralTokens = suppressNumeralTokens;
            return this;
        }
        
        @JsonProperty("diarize_speakers")
        public Builder diarizeSpeakers(boolean diarizeSpeakers) {
            this.diarizeSpeakers = diarizeSpeakers;
            return this;
        }
        
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }
        
        @JsonProperty("min_alignment_score")
        public Builder minAlignmentScore(float minAlignmentScore) {
            this.minAlignmentScore = minAlignmentScore;
            return this;
        }
        
        @JsonProperty("max_alignment_cer")
        public Builder maxAlignmentCer(float maxAlignmentCer) {
            this.maxAlignmentCer = maxAlignmentCer;
            return this;
        }
        
        @JsonProperty("segment_confirmation_silence_threshold")
        public Builder segmentConfirmationSilenceThreshold(float threshold) {
            this.segmentConfirmationSilenceThreshold = threshold;
            return this;
        }
        
        @JsonProperty("only_confirm_by_silence")
        public Builder onlyConfirmBySilence(boolean onlyConfirmBySilence) {
            this.onlyConfirmBySilence = onlyConfirmBySilence;
            return this;
        }
        
        @JsonProperty("batched_inference")
        public Builder batchedInference(boolean batchedInference) {
            this.batchedInference = batchedInference;
            return this;
        }
        
        @JsonProperty("force_detect_language")
        public Builder forceDetectLanguage(boolean forceDetectLanguage) {
            this.forceDetectLanguage = forceDetectLanguage;
            return this;
        }
        
        @JsonProperty("calculate_voice_loudness")
        public Builder calculateVoiceLoudness(boolean calculateVoiceLoudness) {
            this.calculateVoiceLoudness = calculateVoiceLoudness;
            return this;
        }
        
        @JsonProperty("sentence_splitter")
        public Builder sentenceSplitter(SentenceSplitterConfig sentenceSplitter) {
            this.sentenceSplitter = sentenceSplitter;
            return this;
        }
        
        public TranscriptionConfig build() {
            return new TranscriptionConfig(this);
        }
    }
}