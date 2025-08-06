package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Audio format configuration for WebSocket streaming.
 * Defines the format, sample rate, and channels for audio data.
 */
@JsonDeserialize(builder = AudioFormat.Builder.class)
public class AudioFormat {
    private final String format;
    private final int sampleRate;
    private final int channels;
    
    private AudioFormat(Builder builder) {
        this.format = builder.format;
        this.sampleRate = builder.sampleRate;
        this.channels = builder.channels;
    }
    
    @JsonProperty("format")
    public String getFormat() {
        return format;
    }
    
    @JsonProperty("sample_rate")
    public int getSampleRate() {
        return sampleRate;
    }
    
    @JsonProperty("channels")
    public int getChannels() {
        return channels;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String format = ConfigConstants.DEFAULT_AUDIO_FORMAT;
        private int sampleRate = ConfigConstants.DEFAULT_SAMPLE_RATE;
        private int channels = ConfigConstants.DEFAULT_CHANNELS;
        
        public Builder format(String format) {
            if (format == null) {
                throw new IllegalArgumentException("Format cannot be null");
            }
            if (!format.equals("opus") && !format.equals("pcm_s16le") && !format.equals("wav")) {
                throw new IllegalArgumentException("Format must be one of: opus, pcm_s16le, wav");
            }
            this.format = format;
            return this;
        }
        
        @JsonProperty("sample_rate")
        public Builder sampleRate(int sampleRate) {
            if (sampleRate < 16000 || sampleRate > 48000) {
                throw new IllegalArgumentException("Sample rate must be between 16000 and 48000");
            }
            this.sampleRate = sampleRate;
            return this;
        }
        
        public Builder channels(int channels) {
            if (channels != 1 && channels != 2) {
                throw new IllegalArgumentException("Channels must be 1 or 2");
            }
            this.channels = channels;
            return this;
        }
        
        public AudioFormat build() {
            return new AudioFormat(this);
        }
    }
}