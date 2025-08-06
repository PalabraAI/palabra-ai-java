package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Output stream configuration for WebSocket audio streaming.
 */
@JsonDeserialize(builder = OutputStreamConfig.Builder.class)
public class OutputStreamConfig extends StreamConfig {
    private final Map<String, Object> target;
    
    private OutputStreamConfig(Builder builder) {
        super(builder.contentType);
        this.target = new HashMap<>(builder.target);
    }
    
    @JsonProperty("target")
    public Map<String, Object> getTarget() {
        return new HashMap<>(target);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a WebSocket output configuration with the specified format.
     */
    public static OutputStreamConfig websocket(String format) {
        return builder()
            .contentType(ConfigConstants.WEBSOCKET_CONTENT_TYPE)
            .targetType("webrtc")
            .format(format)
            .target(java.util.Map.of(
                "type", "webrtc",
                "format", format,
                "sample_rate", ConfigConstants.DEFAULT_SAMPLE_RATE,
                "channels", ConfigConstants.DEFAULT_CHANNELS
            ))
            .build();
    }
    
    /**
     * Creates a WebSocket output configuration with default format.
     */
    public static OutputStreamConfig websocket() {
        return websocket(ConfigConstants.DEFAULT_AUDIO_FORMAT);
    }
    
    /**
     * Creates a default output stream configuration.
     */
    public static OutputStreamConfig defaults() {
        return websocket();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String contentType = ConfigConstants.WEBSOCKET_CONTENT_TYPE;
        private Map<String, Object> target = new HashMap<>();
        
        public Builder() {
            target.put("type", "webrtc");
            target.put("sample_rate", ConfigConstants.DEFAULT_SAMPLE_RATE);
            target.put("channels", ConfigConstants.DEFAULT_CHANNELS);
        }
        
        @JsonProperty("content_type")
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder targetType(String type) {
            target.put("type", type);
            return this;
        }
        
        public Builder format(String format) {
            if (!format.equals("pcm_s16le") && !format.equals("zlib_pcm_s16le")) {
                throw new IllegalArgumentException("Output format must be pcm_s16le or zlib_pcm_s16le");
            }
            target.put("format", format);
            return this;
        }
        
        @JsonProperty("target")
        public Builder target(Map<String, Object> target) {
            this.target = new HashMap<>(target);
            return this;
        }
        
        public OutputStreamConfig build() {
            return new OutputStreamConfig(this);
        }
    }
}