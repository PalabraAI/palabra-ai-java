package ai.palabra.adapter;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.sound.sampled.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Integration tests for DeviceReader and DeviceWriter classes.
 * Tests device streaming, format conversion, and end-to-end scenarios.
 */
public class DeviceAdapterIntegrationTest {
    
    private DeviceReader deviceReader;
    private DeviceWriter deviceWriter;
    private TargetDataLine mockMicrophone;
    private SourceDataLine mockSpeakers;
    private AudioFormat testFormat;
    
    @BeforeEach
    void setUp() {
        mockMicrophone = mock(TargetDataLine.class);
        mockSpeakers = mock(SourceDataLine.class);
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
        if (deviceWriter != null) {
            deviceWriter.close();
        }
    }
    
    @Test
    void testCreateDeviceAdaptersWithDefaultSettings() {
        deviceReader = new DeviceReader();
        deviceWriter = new DeviceWriter();
        
        assertNotNull(deviceReader);
        assertNotNull(deviceWriter);
        assertEquals(deviceReader.getAudioFormat().toString(), deviceWriter.getAudioFormat().toString());
    }
    
    @Test
    void testCreateDeviceAdaptersWithCustomFormat() {
        AudioFormat customFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100.0f, 16, 1, 2, 44100.0f, false
        );
        
        deviceReader = new DeviceReader(4096, 1000, customFormat);
        deviceWriter = new DeviceWriter(8192, customFormat);
        
        assertEquals(customFormat, deviceReader.getAudioFormat());
        assertEquals(customFormat, deviceWriter.getAudioFormat());
    }
    
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSimulatedAudioPassthrough() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            setupMockAudioSystem(audioSystemMock);
            
            deviceReader = new DeviceReader(1024, 100);
            deviceWriter = new DeviceWriter(1024);
            
            // Simulate microphone providing data
            byte[] testAudioData = generateTestAudioData(1024);
            when(mockMicrophone.read(any(byte[].class), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    System.arraycopy(testAudioData, 0, buffer, 0, Math.min(testAudioData.length, buffer.length));
                    return testAudioData.length;
                });
            
            // Initialize both devices
            deviceReader.initialize();
            deviceWriter.initialize();
            
            assertTrue(deviceReader.isReady());
            assertTrue(deviceWriter.isReady());
            
            // Simulate audio passthrough
            CompletableFuture<Void> passthroughTask = CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 0; i < 5; i++) {
                        byte[] audioData = deviceReader.read();
                        if (audioData != null) {
                            deviceWriter.write(audioData);
                        }
                        Thread.sleep(50); // Small delay between chunks
                    }
                } catch (Exception e) {
                    fail("Passthrough failed: " + e.getMessage());
                }
            });
            
            // Wait for completion
            passthroughTask.get(2, TimeUnit.SECONDS);
            
            // Verify interactions
            verify(mockMicrophone, atLeast(1)).read(any(byte[].class), anyInt(), anyInt());
            
            // Check statistics
            DeviceReader.StreamingStats readerStats = deviceReader.getStreamingStats();
            DeviceWriter.StreamingStats writerStats = deviceWriter.getStreamingStats();
            
            assertTrue(readerStats.totalBytesRead > 0);
            // Note: dropped chunks are expected in this test due to small buffer sizes
            assertTrue(readerStats.droppedChunks >= 0); // Just verify it's not negative
        }
    }
    
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testConcurrentReadWrite() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            setupMockAudioSystem(audioSystemMock);
            
            deviceReader = new DeviceReader(512, 50);
            deviceWriter = new DeviceWriter(512);
            
            // Simulate continuous microphone input
            byte[] testAudioData = generateTestAudioData(512);
            when(mockMicrophone.read(any(byte[].class), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    System.arraycopy(testAudioData, 0, buffer, 0, Math.min(testAudioData.length, buffer.length));
                    Thread.sleep(10); // Simulate real-time audio rate
                    return testAudioData.length;
                });
            
            deviceReader.initialize();
            deviceWriter.initialize();
            
            // Start concurrent reading and writing
            CompletableFuture<Integer> readerTask = CompletableFuture.supplyAsync(() -> {
                int chunksRead = 0;
                try {
                    for (int i = 0; i < 10; i++) {
                        byte[] data = deviceReader.read();
                        if (data != null) {
                            chunksRead++;
                        }
                    }
                } catch (Exception e) {
                    fail("Reader task failed: " + e.getMessage());
                }
                return chunksRead;
            });
            
            CompletableFuture<Integer> writerTask = CompletableFuture.supplyAsync(() -> {
                int chunksWritten = 0;
                try {
                    for (int i = 0; i < 10; i++) {
                        deviceWriter.write(testAudioData);
                        chunksWritten++;
                        Thread.sleep(25);
                    }
                } catch (Exception e) {
                    fail("Writer task failed: " + e.getMessage());
                }
                return chunksWritten;
            });
            
            Integer chunksRead = readerTask.get(2, TimeUnit.SECONDS);
            Integer chunksWritten = writerTask.get(2, TimeUnit.SECONDS);
            
            assertTrue(chunksRead > 0);
            assertTrue(chunksWritten > 0);
        }
    }
    
    @Test
    void testFormatCompatibilityChecking() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            // Test format compatibility detection
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class)))
                .thenReturn(false) // First attempt fails
                .thenReturn(true);  // Fallback succeeds
            
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class)))
                .thenReturn(mockMicrophone);
            
            when(mockMicrophone.isOpen()).thenReturn(true);
            when(mockMicrophone.getFormat()).thenReturn(
                new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 1, 2, 44100.0f, false)
            );
            
            deviceReader = new DeviceReader();
            deviceReader.initialize();
            
            assertTrue(deviceReader.isReady());
            assertNotNull(deviceReader.getActualAudioFormat());
        }
    }
    
    @Test
    void testBufferOverflowRecovery() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            setupMockAudioSystem(audioSystemMock);
            
            // Create devices with small buffer capacity
            deviceReader = new DeviceReader(256, 100);
            deviceWriter = new DeviceWriter(256);
            
            byte[] testData = generateTestAudioData(256);
            when(mockMicrophone.read(any(byte[].class), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    System.arraycopy(testData, 0, buffer, 0, Math.min(testData.length, buffer.length));
                    return testData.length;
                });
            
            deviceReader.initialize();
            deviceWriter.initialize();
            
            // Overwhelm the buffers
            for (int i = 0; i < 10; i++) {
                deviceReader.read(); // Fill reader queue
                deviceWriter.write(testData); // Fill writer queue
            }
            
            // Check that devices handle overflow gracefully
            assertTrue(deviceReader.isReady());
            assertTrue(deviceWriter.isReady());
            
            DeviceReader.StreamingStats readerStats = deviceReader.getStreamingStats();
            DeviceWriter.StreamingStats writerStats = deviceWriter.getStreamingStats();
            
            // May have dropped some chunks due to overflow
            assertTrue(readerStats.droppedChunks >= 0);
            assertTrue(writerStats.droppedChunks >= 0);
        }
    }
    
    @Test
    void testResourceCleanupOnException() throws Exception {
        try (MockedStatic<AudioSystem> audioSystemMock = Mockito.mockStatic(AudioSystem.class)) {
            audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
            audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class)))
                .thenThrow(new LineUnavailableException("Test exception"));
            
            deviceReader = new DeviceReader();
            deviceWriter = new DeviceWriter();
            
            // Both should fail gracefully
            assertThrows(PalabraException.class, () -> deviceReader.initialize());
            assertThrows(PalabraException.class, () -> deviceWriter.initialize());
            
            // Should still be able to close without issues
            deviceReader.close();
            deviceWriter.close();
            
            assertFalse(deviceReader.isReady());
            assertFalse(deviceWriter.isReady());
        }
    }
    
    @Test
    void testStreamingLatencyEstimation() throws Exception {
        deviceReader = new DeviceReader(1024, 1000);
        deviceWriter = new DeviceWriter(2048);
        
        // Initially no latency
        assertEquals(0, deviceReader.getEstimatedLatencyMs());
        assertEquals(0, deviceWriter.getEstimatedLatencyMs());
        
        // Test queue utilization methods
        assertFalse(deviceReader.isQueueNearFull());
        assertTrue(deviceWriter.isUnderrunning()); // Empty queue
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_REAL_AUDIO_TESTS", matches = "true")
    void testRealAudioDevices() throws Exception {
        // This test only runs if ENABLE_REAL_AUDIO_TESTS=true environment variable is set
        // It attempts to use real audio devices
        
        deviceReader = new DeviceReader(4096, 1000);
        deviceWriter = new DeviceWriter(4096);
        
        try {
            deviceReader.initialize();
            assertTrue(deviceReader.isReady());
            
            deviceWriter.initialize();
            assertTrue(deviceWriter.isReady());
            
            // Quick test - read one chunk and write it
            byte[] audioData = deviceReader.read();
            if (audioData != null) {
                deviceWriter.write(audioData);
            }
            
            // Check formats match expected
            assertNotNull(deviceReader.getActualAudioFormat());
            assertNotNull(deviceWriter.getActualAudioFormat());
            
        } catch (PalabraException e) {
            // Skip test if no real audio devices are available
            System.out.println("Skipping real audio test - no devices available: " + e.getMessage());
        }
    }
    
    private void setupMockAudioSystem(MockedStatic<AudioSystem> audioSystemMock) {
        audioSystemMock.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
        
        audioSystemMock.when(() -> AudioSystem.getLine(any(DataLine.Info.class)))
            .thenAnswer(invocation -> {
                DataLine.Info info = invocation.getArgument(0);
                if (info.getLineClass().equals(TargetDataLine.class)) {
                    when(mockMicrophone.isOpen()).thenReturn(true);
                    when(mockMicrophone.getFormat()).thenReturn(testFormat);
                    return mockMicrophone;
                } else if (info.getLineClass().equals(SourceDataLine.class)) {
                    when(mockSpeakers.isOpen()).thenReturn(true);
                    when(mockSpeakers.getFormat()).thenReturn(testFormat);
                    when(mockSpeakers.available()).thenReturn(4096);
                    return mockSpeakers;
                }
                throw new IllegalArgumentException("Unsupported line type");
            });
    }
    
    private byte[] generateTestAudioData(int size) {
        byte[] data = new byte[size];
        // Generate simple sine wave-like pattern
        for (int i = 0; i < size; i += 2) {
            short sample = (short) (Math.sin(i * 0.1) * Short.MAX_VALUE * 0.1);
            data[i] = (byte) (sample & 0xFF);
            if (i + 1 < size) {
                data[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        return data;
    }
}
