package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for stream configuration.
 */
public abstract class StreamConfig {
    protected final String contentType;
    
    protected StreamConfig(String contentType) {
        this.contentType = contentType;
    }
    
    @JsonProperty("content_type")
    public String getContentType() {
        return contentType;
    }
}