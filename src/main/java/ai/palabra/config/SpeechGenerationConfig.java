package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Configuration for speech generation (TTS).
 */
@JsonDeserialize(builder = SpeechGenerationConfig.Builder.class)
public class SpeechGenerationConfig {
    private final String ttsModel;
    private final boolean voiceCloning;
    private final String voiceCloningMode;
    private final boolean denoiseVoiceSamples;
    private final String voiceId;
    private final VoiceTimbreDetectionConfig voiceTimbreDetection;
    private final boolean speechTempoAuto;
    private final int speechTempoTimingsFactor;
    private final float speechTempoAdjustmentFactor;
    private final TTSAdvancedConfig advanced;
    
    private SpeechGenerationConfig(Builder builder) {
        this.ttsModel = builder.ttsModel;
        this.voiceCloning = builder.voiceCloning;
        this.voiceCloningMode = builder.voiceCloningMode;
        this.denoiseVoiceSamples = builder.denoiseVoiceSamples;
        this.voiceId = builder.voiceId;
        this.voiceTimbreDetection = builder.voiceTimbreDetection;
        this.speechTempoAuto = builder.speechTempoAuto;
        this.speechTempoTimingsFactor = builder.speechTempoTimingsFactor;
        this.speechTempoAdjustmentFactor = builder.speechTempoAdjustmentFactor;
        this.advanced = builder.advanced;
    }
    
    @JsonProperty("tts_model")
    public String getTtsModel() {
        return ttsModel;
    }
    
    @JsonProperty("voice_cloning")
    public boolean isVoiceCloning() {
        return voiceCloning;
    }
    
    @JsonProperty("voice_cloning_mode")
    public String getVoiceCloningMode() {
        return voiceCloningMode;
    }
    
    @JsonProperty("denoise_voice_samples")
    public boolean isDenoiseVoiceSamples() {
        return denoiseVoiceSamples;
    }
    
    @JsonProperty("voice_id")
    public String getVoiceId() {
        return voiceId;
    }
    
    @JsonProperty("voice_timbre_detection")
    public VoiceTimbreDetectionConfig getVoiceTimbreDetection() {
        return voiceTimbreDetection;
    }
    
    @JsonProperty("speech_tempo_auto")
    public boolean isSpeechTempoAuto() {
        return speechTempoAuto;
    }
    
    @JsonProperty("speech_tempo_timings_factor")
    public int getSpeechTempoTimingsFactor() {
        return speechTempoTimingsFactor;
    }
    
    @JsonProperty("speech_tempo_adjustment_factor")
    public float getSpeechTempoAdjustmentFactor() {
        return speechTempoAdjustmentFactor;
    }
    
    @JsonProperty("advanced")
    public TTSAdvancedConfig getAdvanced() {
        return advanced;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String ttsModel = ConfigConstants.AUTO_MODEL;
        private boolean voiceCloning = false;
        private String voiceCloningMode = ConfigConstants.VOICE_CLONING_MODE_STATIC_10;
        private boolean denoiseVoiceSamples = true;
        private String voiceId = ConfigConstants.DEFAULT_VOICE_ID;
        private VoiceTimbreDetectionConfig voiceTimbreDetection = VoiceTimbreDetectionConfig.builder().build();
        private boolean speechTempoAuto = true;
        private int speechTempoTimingsFactor = 0;
        private float speechTempoAdjustmentFactor = ConfigConstants.SPEECH_TEMPO_ADJUSTMENT_FACTOR_DEFAULT;
        private TTSAdvancedConfig advanced = TTSAdvancedConfig.builder().build();
        
        @JsonProperty("tts_model")
        public Builder ttsModel(String ttsModel) {
            this.ttsModel = ttsModel;
            return this;
        }
        
        @JsonProperty("voice_cloning")
        public Builder voiceCloning(boolean voiceCloning) {
            this.voiceCloning = voiceCloning;
            return this;
        }
        
        @JsonProperty("voice_cloning_mode")
        public Builder voiceCloningMode(String voiceCloningMode) {
            this.voiceCloningMode = voiceCloningMode;
            return this;
        }
        
        @JsonProperty("denoise_voice_samples")
        public Builder denoiseVoiceSamples(boolean denoiseVoiceSamples) {
            this.denoiseVoiceSamples = denoiseVoiceSamples;
            return this;
        }
        
        @JsonProperty("voice_id")
        public Builder voiceId(String voiceId) {
            this.voiceId = voiceId;
            return this;
        }
        
        @JsonProperty("voice_timbre_detection")
        public Builder voiceTimbreDetection(VoiceTimbreDetectionConfig voiceTimbreDetection) {
            this.voiceTimbreDetection = voiceTimbreDetection;
            return this;
        }
        
        @JsonProperty("speech_tempo_auto")
        public Builder speechTempoAuto(boolean speechTempoAuto) {
            this.speechTempoAuto = speechTempoAuto;
            return this;
        }
        
        @JsonProperty("speech_tempo_timings_factor")
        public Builder speechTempoTimingsFactor(int speechTempoTimingsFactor) {
            this.speechTempoTimingsFactor = speechTempoTimingsFactor;
            return this;
        }
        
        @JsonProperty("speech_tempo_adjustment_factor")
        public Builder speechTempoAdjustmentFactor(float speechTempoAdjustmentFactor) {
            this.speechTempoAdjustmentFactor = speechTempoAdjustmentFactor;
            return this;
        }
        
        public Builder advanced(TTSAdvancedConfig advanced) {
            this.advanced = advanced;
            return this;
        }
        
        public SpeechGenerationConfig build() {
            return new SpeechGenerationConfig(this);
        }
    }
    
    /**
     * Advanced TTS configuration.
     */
    @JsonDeserialize(builder = TTSAdvancedConfig.Builder.class)
    public static class TTSAdvancedConfig {
        private final float f0VarianceFactor;
        private final float energyVarianceFactor;
        private final boolean withCustomStress;
        
        private TTSAdvancedConfig(Builder builder) {
            this.f0VarianceFactor = builder.f0VarianceFactor;
            this.energyVarianceFactor = builder.energyVarianceFactor;
            this.withCustomStress = builder.withCustomStress;
        }
        
        @JsonProperty("f0_variance_factor")
        public float getF0VarianceFactor() {
            return f0VarianceFactor;
        }
        
        @JsonProperty("energy_variance_factor")
        public float getEnergyVarianceFactor() {
            return energyVarianceFactor;
        }
        
        @JsonProperty("with_custom_stress")
        public boolean isWithCustomStress() {
            return withCustomStress;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private float f0VarianceFactor = ConfigConstants.F0_VARIANCE_FACTOR_DEFAULT;
            private float energyVarianceFactor = ConfigConstants.ENERGY_VARIANCE_FACTOR_DEFAULT;
            private boolean withCustomStress = true;
            
            @JsonProperty("f0_variance_factor")
            public Builder f0VarianceFactor(float f0VarianceFactor) {
                this.f0VarianceFactor = f0VarianceFactor;
                return this;
            }
            
            @JsonProperty("energy_variance_factor")
            public Builder energyVarianceFactor(float energyVarianceFactor) {
                this.energyVarianceFactor = energyVarianceFactor;
                return this;
            }
            
            @JsonProperty("with_custom_stress")
            public Builder withCustomStress(boolean withCustomStress) {
                this.withCustomStress = withCustomStress;
                return this;
            }
            
            public TTSAdvancedConfig build() {
                return new TTSAdvancedConfig(this);
            }
        }
    }
}