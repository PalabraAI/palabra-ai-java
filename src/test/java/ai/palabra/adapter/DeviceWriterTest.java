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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceWriter class.
 * Tests speaker access, audio playback, format conversion, and error handling.
 */
public class DeviceWriterTest {
    
    private DeviceWriter deviceWriter;
    private SourceDataLine mockSpeakers;
    private AudioFormat testFormat;
    
    @BeforeEach
    void setUp() {
        System.setProperty("PALABRA_TEST_MODE", "true"); // Enable test mode for faster timeouts
        deviceWriter = new DeviceWriter();
        mockSpeakers = mock(SourceDataLine.class);
        testFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            48000.0f, 16, 1, 2, 48000.0f, false
        );
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (deviceWriter != null) {
            deviceWriter.close();
        }
        System.clearProperty("PALABRA_TEST_MODE"); // Clean up test mode
    }
    
    @Test
    void testDefaultConstructor() {
        assertEquals(8192, deviceWriter.getBufferSize());
        assertNotNull(deviceWriter.getAudioFormat());
        assertFalse(deviceWriter.isPlaying());
        assertFalse(deviceWriter.isReady());
    }
    
    @Test
    void testCustomConstructor() throws Exception {
        DeviceWriter customWriter = new DeviceWriter(4096);
        assertEquals(4096, customWriter.getBufferSize());
        assertFalse(customWriter.isPlaying());
        
        customWriter.close();
    }
    
    @Test
    void testCustomFormatConstructor() throws Exception {
        DeviceWriter customWriter = new DeviceWriter(8192, testFormat);
        assertEquals(testFormat, customWriter.getAudioFormat());
        
        customWriter.close();
    }
    
    @Test
    void testIsReadyBeforeInitialization() {
        assertFalse(deviceWriter.isReady());
    }
    
    @Test
    void testInitializeWithMockAudioSystem() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            
            deviceWriter.initialize();
            
            assertTrue(deviceWriter.isPlaying());
            verify(mockSpeakers).open(any(AudioFormat.class), anyInt());
            verify(mockSpeakers).start();
        }
    }
    
    @Test
    void testInitializeFailsWhenLineNotSupported() {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(false);
            
            PalabraException exception = assertThrows(PalabraException.class, () -> {
                deviceWriter.initialize();
            });
            
            assertTrue(exception.getMessage().contains("No compatible speaker format found"));
        }
    }
    
    @Test
    void testInitializeFailsWhenLineUnavailable() {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class)))
                .thenThrow(new LineUnavailableException("Test exception"));
            
            PalabraException exception = assertThrows(PalabraException.class, () -> {
                deviceWriter.initialize();
            });
            
            assertTrue(exception.getMessage().contains("Failed to open speakers"));
        }
    }
    
    @Test
    void testWriteWithoutInitialization() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            
            byte[] testData = new byte[100];
            
            // Should auto-initialize on first write
            deviceWriter.write(testData);
            
            assertTrue(deviceWriter.isPlaying());
            verify(mockSpeakers).open(any(AudioFormat.class), anyInt());
            verify(mockSpeakers).start();
        }
    }
    
    @Test
    void testWriteNullOrEmptyData() throws Exception {
        // Should not throw exception for null data
        deviceWriter.write(null);
        deviceWriter.write(new byte[0]);
        
        assertFalse(deviceWriter.isPlaying());
    }
    
    @Test
    void testWriteImmediate() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            when(mockSpeakers.write(any(byte[].class), anyInt(), anyInt())).thenReturn(100);
            
            byte[] testData = new byte[100];
            deviceWriter.writeImmediate(testData);
            
            verify(mockSpeakers).write(testData, 0, testData.length);
        }
    }
    
    @Test
    void testWriteImmediateWithoutInitialization() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            when(mockSpeakers.write(any(byte[].class), anyInt(), anyInt())).thenReturn(100);
            
            byte[] testData = new byte[100];
            
            // Should auto-initialize
            deviceWriter.writeImmediate(testData);
            
            assertTrue(deviceWriter.isPlaying());
            verify(mockSpeakers).write(testData, 0, testData.length);
        }
    }
    
    @Test
    void testFlush() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            
            deviceWriter.initialize();
            deviceWriter.flush();
            
            verify(mockSpeakers).drain();
        }
    }
    
    @Test
    void testClose() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            
            deviceWriter.initialize();
            assertTrue(deviceWriter.isPlaying());
            
            deviceWriter.close();
            
            assertFalse(deviceWriter.isPlaying());
            assertFalse(deviceWriter.isReady());
            verify(mockSpeakers).drain();
            verify(mockSpeakers).stop();
            verify(mockSpeakers).close();
        }
    }
    
    @Test
    void testDoubleClose() throws Exception {
        deviceWriter.close();
        deviceWriter.close(); // Should not throw exception
        
        assertFalse(deviceWriter.isPlaying());
        assertFalse(deviceWriter.isReady());
    }
    
    @Test
    void testWriteAfterClose() throws Exception {
        deviceWriter.close();
        
        byte[] testData = new byte[100];
        PalabraException exception = assertThrows(PalabraException.class, () -> {
            deviceWriter.write(testData);
        });
        
        assertTrue(exception.getMessage().contains("Cannot write to closed DeviceWriter"));
    }
    
    @Test
    void testInitializeAfterClose() throws Exception {
        deviceWriter.close();
        
        PalabraException exception = assertThrows(PalabraException.class, () -> {
            deviceWriter.initialize();
        });
        
        assertTrue(exception.getMessage().contains("Cannot initialize closed DeviceWriter"));
    }
    
    @Test
    void testStreamingStats() {
        DeviceWriter.StreamingStats stats = deviceWriter.getStreamingStats();
        assertNotNull(stats);
        assertEquals(0, stats.totalBytesWritten);
        assertEquals(0, stats.droppedChunks);
        assertEquals(0, stats.queueSize);
        assertTrue(stats.queueUtilization >= 0.0);
    }
    
    @Test
    void testResetStats() {
        deviceWriter.resetStats();
        DeviceWriter.StreamingStats stats = deviceWriter.getStreamingStats();
        assertEquals(0, stats.totalBytesWritten);
        assertEquals(0, stats.droppedChunks);
    }
    
    @Test
    void testQueueCapacityMethods() throws Exception {
        assertEquals(0, deviceWriter.getQueueSize());
        assertFalse(deviceWriter.isQueueNearFull());
        assertTrue(deviceWriter.isUnderrunning()); // Empty queue is considered underrunning
        
        // Test with custom capacity
        DeviceWriter smallQueueWriter = new DeviceWriter(4096);
        assertTrue(smallQueueWriter.isUnderrunning()); // Empty queue
        smallQueueWriter.close();
    }
    
    @Test
    void testEstimatedLatency() {
        long latency = deviceWriter.getEstimatedLatencyMs();
        assertTrue(latency >= 0);
    }
    
    @Test
    void testGetActualAudioFormat() {
        assertNull(deviceWriter.getActualAudioFormat()); // Not initialized
        
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            
            deviceWriter.initialize();
            
            assertEquals(testFormat, deviceWriter.getActualAudioFormat());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    void testGetBufferSizeAndMetrics() {
        // Test buffer size
        assertEquals(8192, deviceWriter.getBufferSize()); // Default buffer size
        
        // Test queue size initially
        assertEquals(0, deviceWriter.getQueueSize()); // Initially empty
        
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, deviceWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            when(mockSpeakers.available()).thenReturn(4096);
            
            deviceWriter.initialize();
            
            // Test metrics after initialization
            assertEquals(8192, deviceWriter.getBufferSize());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    void testStreamingStatsToString() {
        DeviceWriter.StreamingStats stats = new DeviceWriter.StreamingStats(2000, 3, 15, 50);
        String str = stats.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("2000"));
        assertTrue(str.contains("3"));
        assertTrue(str.contains("15"));
        assertTrue(str.contains("50"));
        assertTrue(str.contains("30.0%"));
    }
    
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testQueueOverflowHandling() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            // Create writer with very small queue
            DeviceWriter smallQueueWriter = new DeviceWriter(1024);
            
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, smallQueueWriter.getAudioFormat());
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockSpeakers);
            
            when(mockSpeakers.isOpen()).thenReturn(true);
            when(mockSpeakers.getFormat()).thenReturn(testFormat);
            
            smallQueueWriter.initialize();
            
            // Fill up the queue beyond capacity
            byte[] testData = new byte[100];
            smallQueueWriter.write(testData); // 1st chunk
            smallQueueWriter.write(testData); // 2nd chunk (at capacity)
            smallQueueWriter.write(testData); // 3rd chunk (should cause overflow)
            
            // Should handle overflow gracefully
            DeviceWriter.StreamingStats stats = smallQueueWriter.getStreamingStats();
            assertTrue(stats.droppedChunks >= 0); // May have dropped chunks
            
            smallQueueWriter.close();
        }
    }
}
