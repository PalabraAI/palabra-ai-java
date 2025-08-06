package ai.palabra.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.List;

/**
 * Session credentials returned by the Palabra AI API.
 * Contains the necessary tokens and URLs for establishing a translation session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionCredentials {
    
    @JsonProperty("publisher")
    @JsonDeserialize(using = FlexibleStringDeserializer.class)
    private String publisher; // Can come as String or Array from API
    
    @JsonProperty("subscriber") 
    @JsonDeserialize(using = FlexibleStringDeserializer.class)
    private String subscriber; // Can come as String or Array from API
    
    @JsonProperty("room_name")
    private String roomName;
    
    // Support both old and new field names for room
    @JsonProperty("webrtc_room_name")
    private String webrtcRoomName;
    
    @JsonProperty("stream_url")
    private String streamUrl;
    
    // Support both old and new field names for WebRTC URL
    @JsonProperty("webrtc_url")
    private String webrtcUrl;
    
    @JsonProperty("control_url")
    private String controlUrl;
    
    // Support both old and new field names for WebSocket URL
    @JsonProperty("ws_url")
    private String wsUrl;
    
    // New fields from API response
    @JsonProperty("intent")
    private String intent;
    
    @JsonProperty("id")
    private String id;
    
    // Default constructor for Jackson
    public SessionCredentials() {}
    
    /**
     * Constructor for creating SessionCredentials.
     * 
     * @param publisher Publisher token
     * @param subscriber Subscriber token
     * @param roomName LiveKit room name
     * @param streamUrl LiveKit URL
     * @param controlUrl WebSocket management API URL
     */
    public SessionCredentials(String publisher, String subscriber, 
                            String roomName, String streamUrl, String controlUrl) {
        this.publisher = publisher;
        this.subscriber = subscriber;
        this.roomName = roomName;
        this.streamUrl = streamUrl;
        this.controlUrl = controlUrl;
    }
    
    /**
     * Gets the publisher token.
     * 
     * @return Publisher token
     */
    public String getPublisher() {
        return publisher;
    }
    
    /**
     * Sets the publisher token.
     * 
     * @param publisher Publisher token
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    
    /**
     * Gets the subscriber token.
     * 
     * @return Subscriber token
     */
    public String getSubscriber() {
        return subscriber;
    }
    
    /**
     * Sets the subscriber token.
     * 
     * @param subscriber Subscriber token
     */
    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }
    
    /**
     * Gets the LiveKit room name.
     * Tries webrtc_room_name first, then falls back to room_name.
     * 
     * @return Room name
     */
    public String getRoomName() {
        return webrtcRoomName != null ? webrtcRoomName : roomName;
    }
    
    /**
     * Sets the LiveKit room name.
     * 
     * @param roomName Room name
     */
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    
    /**
     * Gets the WebRTC room name (new API field).
     * 
     * @return WebRTC room name
     */
    public String getWebrtcRoomName() {
        return webrtcRoomName;
    }
    
    /**
     * Sets the WebRTC room name.
     * 
     * @param webrtcRoomName WebRTC room name
     */
    public void setWebrtcRoomName(String webrtcRoomName) {
        this.webrtcRoomName = webrtcRoomName;
    }
    
    /**
     * Gets the LiveKit stream URL.
     * Tries webrtc_url first, then falls back to stream_url.
     * 
     * @return Stream URL
     */
    public String getStreamUrl() {
        return webrtcUrl != null ? webrtcUrl : streamUrl;
    }
    
    /**
     * Sets the LiveKit stream URL.
     * 
     * @param streamUrl Stream URL
     */
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
    
    /**
     * Gets the WebRTC URL (new API field).
     * 
     * @return WebRTC URL
     */
    public String getWebrtcUrl() {
        return webrtcUrl;
    }
    
    /**
     * Sets the WebRTC URL.
     * 
     * @param webrtcUrl WebRTC URL
     */
    public void setWebrtcUrl(String webrtcUrl) {
        this.webrtcUrl = webrtcUrl;
    }
    
    /**
     * Gets the WebSocket management API URL.
     * Tries ws_url first, then falls back to control_url.
     * 
     * @return Control URL
     */
    public String getControlUrl() {
        return wsUrl != null ? wsUrl : controlUrl;
    }
    
    /**
     * Sets the WebSocket management API URL.
     * 
     * @param controlUrl Control URL
     */
    public void setControlUrl(String controlUrl) {
        this.controlUrl = controlUrl;
    }
    
    /**
     * Gets the WebSocket URL (new API field).
     * 
     * @return WebSocket URL
     */
    public String getWsUrl() {
        return wsUrl;
    }
    
    /**
     * Sets the WebSocket URL.
     * 
     * @param wsUrl WebSocket URL
     */
    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }
    
    /**
     * Gets the intent field from API response.
     * 
     * @return Intent
     */
    public String getIntent() {
        return intent;
    }
    
    /**
     * Sets the intent field.
     * 
     * @param intent Intent
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    /**
     * Gets the ID field from API response.
     * 
     * @return ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the ID field.
     * 
     * @param id ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "SessionCredentials{" +
                "publisher='" + publisher + '\'' +
                ", subscriber='" + subscriber + '\'' +
                ", roomName='" + getRoomName() + '\'' +
                ", streamUrl='" + getStreamUrl() + '\'' +
                ", controlUrl='" + getControlUrl() + '\'' +
                ", intent='" + intent + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
    
    /**
     * Custom deserializer that handles both String and Array formats
     * for publisher/subscriber fields from the API.
     */
    public static class FlexibleStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            if (node.isTextual()) {
                // If it's a string, return it directly
                return node.asText();
            } else if (node.isArray() && node.size() > 0) {
                // If it's an array, return the first element
                return node.get(0).asText();
            } else {
                // Default fallback
                return null;
            }
        }
    }
}
