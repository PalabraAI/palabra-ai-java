package ai.palabra.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Configuration for translation queue management.
 */
@JsonDeserialize(builder = QueueConfig.Builder.class)
public class QueueConfig {
    private final int desiredQueueLevelMs;
    private final int maxQueueLevelMs;
    private final boolean autoTempo;
    
    private QueueConfig(Builder builder) {
        this.desiredQueueLevelMs = builder.desiredQueueLevelMs;
        this.maxQueueLevelMs = builder.maxQueueLevelMs;
        this.autoTempo = builder.autoTempo;
    }
    
    @JsonProperty("desired_queue_level_ms")
    public int getDesiredQueueLevelMs() {
        return desiredQueueLevelMs;
    }
    
    @JsonProperty("max_queue_level_ms")
    public int getMaxQueueLevelMs() {
        return maxQueueLevelMs;
    }
    
    @JsonProperty("auto_tempo")
    public boolean isAutoTempo() {
        return autoTempo;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private int desiredQueueLevelMs = ConfigConstants.DESIRED_QUEUE_LEVEL_MS_DEFAULT;
        private int maxQueueLevelMs = ConfigConstants.MAX_QUEUE_LEVEL_MS_DEFAULT;
        private boolean autoTempo = true;
        
        @JsonProperty("desired_queue_level_ms")
        public Builder desiredQueueLevelMs(int desiredQueueLevelMs) {
            if (desiredQueueLevelMs < 0) {
                throw new IllegalArgumentException("Desired queue level must be positive");
            }
            this.desiredQueueLevelMs = desiredQueueLevelMs;
            return this;
        }
        
        @JsonProperty("max_queue_level_ms")
        public Builder maxQueueLevelMs(int maxQueueLevelMs) {
            if (maxQueueLevelMs < 0) {
                throw new IllegalArgumentException("Max queue level must be positive");
            }
            this.maxQueueLevelMs = maxQueueLevelMs;
            return this;
        }
        
        @JsonProperty("auto_tempo")
        public Builder autoTempo(boolean autoTempo) {
            this.autoTempo = autoTempo;
            return this;
        }
        
        public QueueConfig build() {
            if (maxQueueLevelMs < desiredQueueLevelMs) {
                throw new IllegalStateException("Max queue level must be >= desired queue level");
            }
            return new QueueConfig(this);
        }
    }
}