package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Configuration for sentence splitting in transcription.
 */
@JsonDeserialize(builder = SentenceSplitterConfig.Builder.class)
public class SentenceSplitterConfig {
    private final boolean enabled;
    private final String splitterModel;
    private final SplitterAdvancedConfig advanced;
    
    private SentenceSplitterConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.splitterModel = builder.splitterModel;
        this.advanced = builder.advanced;
    }
    
    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }
    
    @JsonProperty("splitter_model")
    public String getSplitterModel() {
        return splitterModel;
    }
    
    @JsonProperty("advanced")
    public SplitterAdvancedConfig getAdvanced() {
        return advanced;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private boolean enabled = true;
        private String splitterModel = ConfigConstants.AUTO_MODEL;
        private SplitterAdvancedConfig advanced = SplitterAdvancedConfig.builder().build();
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        @JsonProperty("splitter_model")
        public Builder splitterModel(String splitterModel) {
            this.splitterModel = splitterModel;
            return this;
        }
        
        public Builder advanced(SplitterAdvancedConfig advanced) {
            this.advanced = advanced;
            return this;
        }
        
        public SentenceSplitterConfig build() {
            return new SentenceSplitterConfig(this);
        }
    }
    
    /**
     * Advanced configuration for sentence splitter.
     */
    @JsonDeserialize(builder = SplitterAdvancedConfig.Builder.class)
    public static class SplitterAdvancedConfig {
        private final int minSentenceCharacters;
        private final int minSentenceSeconds;
        private final float minSplitInterval;
        private final int contextSize;
        private final int segmentsAfterRestart;
        private final int stepSize;
        private final int maxStepsWithoutEos;
        private final float forceEndOfSegment;
        
        private SplitterAdvancedConfig(Builder builder) {
            this.minSentenceCharacters = builder.minSentenceCharacters;
            this.minSentenceSeconds = builder.minSentenceSeconds;
            this.minSplitInterval = builder.minSplitInterval;
            this.contextSize = builder.contextSize;
            this.segmentsAfterRestart = builder.segmentsAfterRestart;
            this.stepSize = builder.stepSize;
            this.maxStepsWithoutEos = builder.maxStepsWithoutEos;
            this.forceEndOfSegment = builder.forceEndOfSegment;
        }
        
        @JsonProperty("min_sentence_characters")
        public int getMinSentenceCharacters() {
            return minSentenceCharacters;
        }
        
        @JsonProperty("min_sentence_seconds")
        public int getMinSentenceSeconds() {
            return minSentenceSeconds;
        }
        
        @JsonProperty("min_split_interval")
        public float getMinSplitInterval() {
            return minSplitInterval;
        }
        
        @JsonProperty("context_size")
        public int getContextSize() {
            return contextSize;
        }
        
        @JsonProperty("segments_after_restart")
        public int getSegmentsAfterRestart() {
            return segmentsAfterRestart;
        }
        
        @JsonProperty("step_size")
        public int getStepSize() {
            return stepSize;
        }
        
        @JsonProperty("max_steps_without_eos")
        public int getMaxStepsWithoutEos() {
            return maxStepsWithoutEos;
        }
        
        @JsonProperty("force_end_of_segment")
        public float getForceEndOfSegment() {
            return forceEndOfSegment;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private int minSentenceCharacters = ConfigConstants.MIN_SENTENCE_CHARACTERS_DEFAULT;
            private int minSentenceSeconds = ConfigConstants.MIN_SENTENCE_SECONDS_DEFAULT;
            private float minSplitInterval = ConfigConstants.MIN_SPLIT_INTERVAL_DEFAULT;
            private int contextSize = ConfigConstants.CONTEXT_SIZE_DEFAULT;
            private int segmentsAfterRestart = ConfigConstants.SEGMENTS_AFTER_RESTART_DEFAULT;
            private int stepSize = ConfigConstants.STEP_SIZE_DEFAULT;
            private int maxStepsWithoutEos = ConfigConstants.MAX_STEPS_WITHOUT_EOS_DEFAULT;
            private float forceEndOfSegment = ConfigConstants.FORCE_END_OF_SEGMENT_DEFAULT;
            
            @JsonProperty("min_sentence_characters")
            public Builder minSentenceCharacters(int minSentenceCharacters) {
                this.minSentenceCharacters = minSentenceCharacters;
                return this;
            }
            
            @JsonProperty("min_sentence_seconds")
            public Builder minSentenceSeconds(int minSentenceSeconds) {
                this.minSentenceSeconds = minSentenceSeconds;
                return this;
            }
            
            @JsonProperty("min_split_interval")
            public Builder minSplitInterval(float minSplitInterval) {
                this.minSplitInterval = minSplitInterval;
                return this;
            }
            
            @JsonProperty("context_size")
            public Builder contextSize(int contextSize) {
                this.contextSize = contextSize;
                return this;
            }
            
            @JsonProperty("segments_after_restart")
            public Builder segmentsAfterRestart(int segmentsAfterRestart) {
                this.segmentsAfterRestart = segmentsAfterRestart;
                return this;
            }
            
            @JsonProperty("step_size")
            public Builder stepSize(int stepSize) {
                this.stepSize = stepSize;
                return this;
            }
            
            @JsonProperty("max_steps_without_eos")
            public Builder maxStepsWithoutEos(int maxStepsWithoutEos) {
                this.maxStepsWithoutEos = maxStepsWithoutEos;
                return this;
            }
            
            @JsonProperty("force_end_of_segment")
            public Builder forceEndOfSegment(float forceEndOfSegment) {
                this.forceEndOfSegment = forceEndOfSegment;
                return this;
            }
            
            public SplitterAdvancedConfig build() {
                return new SplitterAdvancedConfig(this);
            }
        }
    }
}