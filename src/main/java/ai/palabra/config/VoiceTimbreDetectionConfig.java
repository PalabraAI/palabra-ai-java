package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for voice timbre detection.
 */
@JsonDeserialize(builder = VoiceTimbreDetectionConfig.Builder.class)
public class VoiceTimbreDetectionConfig {
    private final boolean enabled;
    private final List<String> highTimbreVoices;
    private final List<String> lowTimbreVoices;
    
    private VoiceTimbreDetectionConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.highTimbreVoices = new ArrayList<>(builder.highTimbreVoices);
        this.lowTimbreVoices = new ArrayList<>(builder.lowTimbreVoices);
    }
    
    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }
    
    @JsonProperty("high_timbre_voices")
    public List<String> getHighTimbreVoices() {
        return new ArrayList<>(highTimbreVoices);
    }
    
    @JsonProperty("low_timbre_voices")
    public List<String> getLowTimbreVoices() {
        return new ArrayList<>(lowTimbreVoices);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private boolean enabled = true;
        private List<String> highTimbreVoices = new ArrayList<>(Arrays.asList(ConfigConstants.DEFAULT_HIGH_TIMBRE_VOICE));
        private List<String> lowTimbreVoices = new ArrayList<>(Arrays.asList(ConfigConstants.DEFAULT_LOW_TIMBRE_VOICE));
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        @JsonProperty("high_timbre_voices")
        public Builder highTimbreVoices(List<String> highTimbreVoices) {
            this.highTimbreVoices = new ArrayList<>(highTimbreVoices);
            return this;
        }
        
        @JsonProperty("low_timbre_voices")
        public Builder lowTimbreVoices(List<String> lowTimbreVoices) {
            this.lowTimbreVoices = new ArrayList<>(lowTimbreVoices);
            return this;
        }
        
        public VoiceTimbreDetectionConfig build() {
            return new VoiceTimbreDetectionConfig(this);
        }
    }
}