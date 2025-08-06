package ai.palabra.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionCredentials data class.
 */
class SessionCredentialsTest {
    
    @Test
    void testDefaultConstructor() {
        SessionCredentials credentials = new SessionCredentials();
        assertNull(credentials.getPublisher());
        assertNull(credentials.getSubscriber());
        assertNull(credentials.getRoomName());
        assertNull(credentials.getStreamUrl());
        assertNull(credentials.getControlUrl());
        assertNull(credentials.getIntent());
    }
    
    @Test
    void testParameterizedConstructor() {
        String publisher = "pub-token";
        String subscriber = "sub-token";
        String roomName = "test-room";
        String streamUrl = "wss://stream.test.com";
        String controlUrl = "wss://control.test.com";
        
        SessionCredentials credentials = new SessionCredentials(
            publisher, subscriber, roomName, streamUrl, controlUrl
        );
        
        assertEquals(publisher, credentials.getPublisher());
        assertEquals(subscriber, credentials.getSubscriber());
        assertEquals(roomName, credentials.getRoomName());
        assertEquals(streamUrl, credentials.getStreamUrl());
        assertEquals(controlUrl, credentials.getControlUrl());
    }
    
    @Test
    void testSettersAndGetters() {
        SessionCredentials credentials = new SessionCredentials();
        
        String publisher = "publisher-token-1";
        String subscriber = "subscriber-token-1";
        String roomName = "my-room";
        String streamUrl = "wss://stream.palabra.ai";
        String controlUrl = "wss://control.palabra.ai";
        String intent = "translate";
        
        credentials.setPublisher(publisher);
        credentials.setSubscriber(subscriber);
        credentials.setRoomName(roomName);
        credentials.setStreamUrl(streamUrl);
        credentials.setControlUrl(controlUrl);
        credentials.setIntent(intent);
        
        assertEquals(publisher, credentials.getPublisher());
        assertEquals(subscriber, credentials.getSubscriber());
        assertEquals(roomName, credentials.getRoomName());
        assertEquals(streamUrl, credentials.getStreamUrl());
        assertEquals(controlUrl, credentials.getControlUrl());
        assertEquals(intent, credentials.getIntent());
    }
    
    @Test
    void testToString() {
        String publisher = "pub";
        String subscriber = "sub";
        SessionCredentials credentials = new SessionCredentials(
            publisher, subscriber, "room", "stream", "control"
        );
        credentials.setIntent("translate");
        
        String result = credentials.toString();
        assertTrue(result.contains("publisher='pub'") || result.contains("publisher=pub"));
        assertTrue(result.contains("subscriber='sub'") || result.contains("subscriber=sub"));
        assertTrue(result.contains("roomName='room'"));
        assertTrue(result.contains("streamUrl='stream'"));
        assertTrue(result.contains("controlUrl='control'"));
    }
    
    @Test
    void testIntentField() {
        SessionCredentials credentials = new SessionCredentials();
        assertNull(credentials.getIntent());
        
        credentials.setIntent("translate");
        assertEquals("translate", credentials.getIntent());
        
        credentials.setIntent(null);
        assertNull(credentials.getIntent());
    }
    
    @Test
    void testWebRtcFields() {
        SessionCredentials credentials = new SessionCredentials();
        
        // Test WebRTC room name priority
        credentials.setRoomName("old-room");
        credentials.setWebrtcRoomName("new-room");
        assertEquals("new-room", credentials.getRoomName()); // Should prefer webrtc_room_name
        
        // Test WebRTC URL priority
        credentials.setStreamUrl("old-url");
        credentials.setWebrtcUrl("new-url");
        assertEquals("new-url", credentials.getStreamUrl()); // Should prefer webrtc_url
        
        // Test WebSocket URL priority
        credentials.setControlUrl("old-control");
        credentials.setWsUrl("new-ws");
        assertEquals("new-ws", credentials.getControlUrl()); // Should prefer ws_url
    }
}
