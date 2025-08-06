package ai.palabra.adapter;

import ai.palabra.PalabraException;
import ai.palabra.util.AudioUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileReader class.
 */
class FileReaderTest {

    @TempDir
    Path tempDir;
    
    private Path testAudioFile;
    private FileReader fileReader;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a simple test WAV file
        testAudioFile = tempDir.resolve("test_audio.wav");
        createTestWavFile(testAudioFile);
    }
    
    @AfterEach
    void tearDown() throws PalabraException {
        if (fileReader != null) {
            fileReader.close();
        }
    }
    
    @Test
    void testConstructor() {
        fileReader = new FileReader(testAudioFile.toString());
        
        assertEquals(testAudioFile.toString(), fileReader.getFilePath());
        assertFalse(fileReader.isReady());
    }
    
    @Test
    void testInitializeWithValidFile() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        
        fileReader.initialize();
        
        assertTrue(fileReader.isReady());
        assertTrue(fileReader.getTotalSize() > 0);
        assertEquals(0, fileReader.getPosition());
        assertFalse(fileReader.isEndOfFile());
    }
    
    @Test
    void testInitializeWithNonExistentFile() {
        Path nonExistentFile = tempDir.resolve("nonexistent.wav");
        fileReader = new FileReader(nonExistentFile.toString());
        
        PalabraException exception = assertThrows(PalabraException.class, () -> {
            fileReader.initialize();
        });
        
        assertTrue(exception.getMessage().contains("Audio file not found"));
    }
    
    @Test
    void testReadWithoutInitialize() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        
        // Should auto-initialize
        byte[] data = fileReader.read();
        
        assertNotNull(data);
        assertTrue(data.length > 0);
        assertTrue(fileReader.isReady());
    }
    
    @Test
    void testReadSequential() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        fileReader.initialize();
        
        int totalBytesRead = 0;
        byte[] chunk;
        
        while ((chunk = fileReader.read(1024)) != null) {
            totalBytesRead += chunk.length;
        }
        
        assertTrue(totalBytesRead > 0);
        assertTrue(fileReader.isEndOfFile());
        assertEquals(fileReader.getTotalSize(), totalBytesRead);
    }
    
    @Test
    void testReadSpecificSize() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        fileReader.initialize();
        
        byte[] chunk = fileReader.read(512);
        
        assertNotNull(chunk);
        assertTrue(chunk.length <= 512);
        assertEquals(chunk.length, fileReader.getPosition());
    }
    
    @Test
    void testReset() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        fileReader.initialize();
        
        // Read some data
        fileReader.read(1024);
        assertTrue(fileReader.getPosition() > 0);
        
        // Reset
        fileReader.reset();
        assertEquals(0, fileReader.getPosition());
        assertFalse(fileReader.isEndOfFile());
    }
    
    @Test
    void testClose() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        fileReader.initialize();
        
        assertTrue(fileReader.isReady());
        
        fileReader.close();
        
        assertFalse(fileReader.isReady());
        assertEquals(0, fileReader.getPosition());
        assertEquals(-1, fileReader.getTotalSize());
    }
    
    @Test
    void testReadAfterEndOfFile() throws PalabraException {
        fileReader = new FileReader(testAudioFile.toString());
        fileReader.initialize();
        
        // Read all data
        while (fileReader.read(1024) != null) {
            // Continue reading until end
        }
        
        assertTrue(fileReader.isEndOfFile());
        
        // Try to read more
        byte[] data = fileReader.read();
        assertNull(data);
    }
    
    /**
     * Creates a simple test WAV file with PCM data.
     */
    private void createTestWavFile(Path filePath) throws IOException {
        // Create simple PCM audio data (1 second of silence at 48kHz, 16-bit, mono)
        int sampleRate = 48000;
        int duration = 1; // seconds
        int totalSamples = sampleRate * duration;
        byte[] pcmData = new byte[totalSamples * 2]; // 16-bit = 2 bytes per sample
        
        // Fill with simple sine wave for testing
        for (int i = 0; i < totalSamples; i++) {
            short sample = (short) (Math.sin(2.0 * Math.PI * 440.0 * i / sampleRate) * Short.MAX_VALUE / 4);
            pcmData[i * 2] = (byte) (sample & 0xff);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        
        // Write as WAV file
        AudioUtils.writePcmToWav(pcmData, filePath);
    }
}
