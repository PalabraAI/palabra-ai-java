package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Input stream configuration for WebSocket audio streaming.
 */
@JsonDeserialize(builder = InputStreamConfig.Builder.class)
public class InputStreamConfig extends StreamConfig {
    private final Map<String, Object> source;
    
    private InputStreamConfig(Builder builder) {
        super(builder.contentType);
        this.source = new HashMap<>(builder.source);
    }
    
    @JsonProperty("source")
    public Map<String, Object> getSource() {
        return new HashMap<>(source);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a WebSocket input configuration with the specified audio format.
     */
    public static InputStreamConfig websocket(AudioFormat audioFormat) {
        return builder()
            .contentType(ConfigConstants.WEBSOCKET_CONTENT_TYPE)
            .sourceType(ConfigConstants.WEBSOCKET_CONTENT_TYPE)
            .audioFormat(audioFormat)
            .build();
    }
    
    /**
     * Creates a WebSocket input configuration with default audio format.
     */
    public static InputStreamConfig websocket() {
        return websocket(AudioFormat.builder().build());
    }
    
    /**
     * Creates a default input stream configuration.
     */
    public static InputStreamConfig defaults() {
        return websocket();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String contentType = ConfigConstants.WEBSOCKET_CONTENT_TYPE;
        private Map<String, Object> source = new HashMap<>();
        
        public Builder() {
            source.put("type", ConfigConstants.WEBSOCKET_CONTENT_TYPE);
        }
        
        @JsonProperty("content_type")
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder sourceType(String type) {
            source.put("type", type);
            return this;
        }
        
        public Builder audioFormat(AudioFormat audioFormat) {
            source.put("format", audioFormat.getFormat());
            source.put("sample_rate", audioFormat.getSampleRate());
            source.put("channels", audioFormat.getChannels());
            return this;
        }
        
        @JsonProperty("source")
        public Builder source(Map<String, Object> source) {
            this.source = new HashMap<>(source);
            return this;
        }
        
        public InputStreamConfig build() {
            return new InputStreamConfig(this);
        }
    }
}