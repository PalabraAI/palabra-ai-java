package ai.palabra.adapter;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.sound.sampled.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceReader class.
 * Tests microphone access, audio capture, format conversion, and error handling.
 */
public class DeviceReaderTest {
    
    private DeviceReader deviceReader;
    private TargetDataLine mockMicrophone;
    private AudioFormat testFormat;
    
    @BeforeEach
    void setUp() {
        deviceReader = new DeviceReader();
        mockMicrophone = mock(TargetDataLine.class);
        testFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            48000.0f, 16, 1, 2, 48000.0f, false
        );
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (deviceReader != null) {
            deviceReader.close();
        }
    }
    
    @Test
    void testDefaultConstructor() {
        assertEquals(4096, deviceReader.getChunkSize());
        assertNotNull(deviceReader.getAudioFormat());
        assertFalse(deviceReader.isRecording());
        assertFalse(deviceReader.isReady());
    }
    
    @Test
    void testCustomConstructor() throws Exception {
        DeviceReader customReader = new DeviceReader(2048, 500);
        assertEquals(2048, customReader.getChunkSize());
        assertFalse(customReader.isRecording());
        
        customReader.close();
    }
    
    @Test
    void testCustomFormatConstructor() throws Exception {
        DeviceReader customReader = new DeviceReader(4096, 1000, testFormat);
        assertEquals(testFormat, customReader.getAudioFormat());
        
        customReader.close();
    }
    
    @Test
    void testIsReadyBeforeInitialization() {
        assertFalse(deviceReader.isReady());
    }
    
    @Test
    void testInitializeWithMockAudioSystem() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, deviceReader.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockMicrophone);
            
            when(mockMicrophone.isOpen()).thenReturn(true);
            when(mockMicrophone.getFormat()).thenReturn(testFormat);
            
            deviceReader.initialize();
            
            assertTrue(deviceReader.isRecording());
            verify(mockMicrophone).open(any(AudioFormat.class), anyInt());
            verify(mockMicrophone).start();
        }
    }
    
    @Test
    void testInitializeFailsWhenLineNotSupported() {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(false);
            
            PalabraException exception = assertThrows(PalabraException.class, () -> {
                deviceReader.initialize();
            });
            
            assertTrue(exception.getMessage().contains("No compatible microphone format found"));
        }
    }
    
    @Test
    void testInitializeFailsWhenLineUnavailable() {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class)))
                .thenThrow(new LineUnavailableException("Test exception"));
            
            PalabraException exception = assertThrows(PalabraException.class, () -> {
                deviceReader.initialize();
            });
            
            assertTrue(exception.getMessage().contains("Failed to open microphone"));
        }
    }
    
    @Test
    void testReadWithoutInitialization() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, deviceReader.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockMicrophone);
            
            when(mockMicrophone.isOpen()).thenReturn(true);
            when(mockMicrophone.getFormat()).thenReturn(testFormat);
            when(mockMicrophone.read(any(byte[].class), anyInt(), anyInt())).thenReturn(100);
            
            // Should auto-initialize on first read
            byte[] data = deviceReader.read();
            
            assertTrue(deviceReader.isRecording());
            verify(mockMicrophone).open(any(AudioFormat.class), anyInt());
            verify(mockMicrophone).start();
        }
    }
    
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testReadTimeout() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DeviceReader shortTimeoutReader = new DeviceReader(4096, 100); // 100ms timeout
            
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, shortTimeoutReader.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockMicrophone);
            
            when(mockMicrophone.isOpen()).thenReturn(true);
            when(mockMicrophone.getFormat()).thenReturn(testFormat);
            // Don't return any data from microphone, should timeout
            when(mockMicrophone.read(any(byte[].class), anyInt(), anyInt())).thenReturn(0);
            
            shortTimeoutReader.initialize();
            byte[] data = shortTimeoutReader.read();
            
            assertNull(data); // Should return null on timeout
            shortTimeoutReader.close();
        }
    }
    
    @Test
    void testClose() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, deviceReader.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockMicrophone);
            
            when(mockMicrophone.isOpen()).thenReturn(true);
            when(mockMicrophone.getFormat()).thenReturn(testFormat);
            
            deviceReader.initialize();
            assertTrue(deviceReader.isRecording());
            
            deviceReader.close();
            
            assertFalse(deviceReader.isRecording());
            assertFalse(deviceReader.isReady());
            verify(mockMicrophone).stop();
            verify(mockMicrophone).close();
        }
    }
    
    @Test
    void testDoubleClose() throws Exception {
        deviceReader.close();
        deviceReader.close(); // Should not throw exception
        
        assertFalse(deviceReader.isRecording());
        assertFalse(deviceReader.isReady());
    }
    
    @Test
    void testReadAfterClose() throws Exception {
        deviceReader.close();
        
        byte[] data = deviceReader.read();
        assertNull(data);
    }
    
    @Test
    void testInitializeAfterClose() throws Exception {
        deviceReader.close();
        
        PalabraException exception = assertThrows(PalabraException.class, () -> {
            deviceReader.initialize();
        });
        
        assertTrue(exception.getMessage().contains("Cannot initialize closed DeviceReader"));
    }
    
    @Test
    void testStreamingStats() {
        DeviceReader.StreamingStats stats = deviceReader.getStreamingStats();
        assertNotNull(stats);
        assertEquals(0, stats.totalBytesRead);
        assertEquals(0, stats.droppedChunks);
        assertEquals(0, stats.queueSize);
        assertTrue(stats.queueUtilization >= 0.0);
    }
    
    @Test
    void testResetStats() {
        deviceReader.resetStats();
        DeviceReader.StreamingStats stats = deviceReader.getStreamingStats();
        assertEquals(0, stats.totalBytesRead);
        assertEquals(0, stats.droppedChunks);
    }
    
    @Test
    void testQueueCapacityMethods() throws Exception {
        assertEquals(0, deviceReader.getQueueSize());
        assertFalse(deviceReader.isQueueNearFull());
        
        // Test with custom capacity
        DeviceReader smallQueueReader = new DeviceReader(4096, 1000, deviceReader.getAudioFormat());
        assertFalse(smallQueueReader.isQueueNearFull());
        smallQueueReader.close();
    }
    
    @Test
    void testEstimatedLatency() {
        long latency = deviceReader.getEstimatedLatencyMs();
        assertTrue(latency >= 0);
    }
    
    @Test
    void testGetActualAudioFormat() {
        assertNull(deviceReader.getActualAudioFormat()); // Not initialized
        
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, deviceReader.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockMicrophone);
            
            when(mockMicrophone.isOpen()).thenReturn(true);
            when(mockMicrophone.getFormat()).thenReturn(testFormat);
            
            deviceReader.initialize();
            
            assertEquals(testFormat, deviceReader.getActualAudioFormat());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    void testStreamingStatsToString() {
        DeviceReader.StreamingStats stats = new DeviceReader.StreamingStats(1000, 5, 10, 50);
        String str = stats.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("1000"));
        assertTrue(str.contains("5"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("50"));
        assertTrue(str.contains("20.0%"));
    }
}
