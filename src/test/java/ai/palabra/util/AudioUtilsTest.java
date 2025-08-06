package ai.palabra.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AudioUtils class.
 */
class AudioUtilsTest {

    @TempDir
    Path tempDir;
    
    @Test
    void testGetPcmFormat() {
        AudioFormat format = AudioUtils.getPcmFormat();
        
        assertNotNull(format);
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        assertEquals(48000.0f, format.getSampleRate());
        assertEquals(16, format.getSampleSizeInBits());
        assertEquals(1, format.getChannels());
        assertEquals(2, format.getFrameSize());
        assertFalse(format.isBigEndian());
    }
    
    @Test
    void testGetSupportedFormats() {
        String[] formats = AudioUtils.getSupportedFormats();
        
        assertNotNull(formats);
        assertTrue(formats.length > 0);
        
        // Should at least support WAV
        boolean supportsWav = false;
        for (String format : formats) {
            if ("wav".equalsIgnoreCase(format)) {
                supportsWav = true;
                break;
            }
        }
        assertTrue(supportsWav, "Should support WAV format");
    }
    
    @Test
    void testConvertToPcm16WithValidPcmData() throws IOException {
        // Create simple PCM data
        byte[] originalData = createTestPcmData(1024);
        
        byte[] convertedData = AudioUtils.convertToPcm16(originalData);
        
        assertNotNull(convertedData);
        // For raw PCM data that's not a recognized file format, should return as-is
        assertArrayEquals(originalData, convertedData);
    }
    
    @Test
    void testWritePcmToWav() throws IOException {
        Path outputFile = tempDir.resolve("test_output.wav");
        byte[] pcmData = createTestPcmData(2048);
        
        AudioUtils.writePcmToWav(pcmData, outputFile);
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > pcmData.length); // WAV header adds size
    }
    
    @Test
    void testWritePcmToWavWithEmptyData() throws IOException {
        Path outputFile = tempDir.resolve("empty_output.wav");
        byte[] emptyData = new byte[0];
        
        AudioUtils.writePcmToWav(emptyData, outputFile);
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0); // Still has WAV header
    }
    
    @Test
    void testWritePcmToWavWithNullData() throws IOException {
        Path outputFile = tempDir.resolve("null_output.wav");
        
        AudioUtils.writePcmToWav(null, outputFile);
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0); // Still has WAV header
    }
    
    @Test
    void testResampleAudioSameRate() {
        byte[] originalData = createTestPcmData(1000);
        
        byte[] resampledData = AudioUtils.resampleAudio(originalData, 48000.0f, 48000.0f);
        
        assertArrayEquals(originalData, resampledData);
    }
    
    @Test
    void testResampleAudioDifferentRate() {
        byte[] originalData = createTestPcmData(1000);
        
        byte[] resampledData = AudioUtils.resampleAudio(originalData, 48000.0f, 24000.0f);
        
        assertNotNull(resampledData);
        // Should be roughly half the size (due to halving sample rate)
        assertTrue(resampledData.length < originalData.length);
        assertTrue(resampledData.length > originalData.length / 3); // Allow some tolerance
    }
    
    @Test
    void testStereoToMono() {
        // Create stereo data (4 samples, 2 channels each = 8 samples total = 16 bytes)
        byte[] stereoData = new byte[16];
        for (int i = 0; i < stereoData.length; i += 4) {
            // Left channel sample
            stereoData[i] = 100;
            stereoData[i + 1] = 0;
            // Right channel sample  
            stereoData[i + 2] = 50;
            stereoData[i + 3] = 0;
        }
        
        byte[] monoData = AudioUtils.stereoToMono(stereoData);
        
        assertNotNull(monoData);
        assertEquals(8, monoData.length); // Half the size
        
        // Check that values are averaged correctly
        assertEquals(75, monoData[0] & 0xFF); // (100 + 50) / 2
        assertEquals(0, monoData[1] & 0xFF);
    }
    
    @Test
    void testReadAudioFileNonExistent() {
        Path nonExistentFile = tempDir.resolve("nonexistent.wav");
        
        IOException exception = assertThrows(IOException.class, () -> {
            AudioUtils.readAudioFile(nonExistentFile);
        });
        
        assertTrue(exception.getMessage().contains("Audio file not found"));
    }
    
    @Test
    void testCreateDirectoryStructure() throws IOException {
        Path nestedPath = tempDir.resolve("nested").resolve("directory").resolve("test.wav");
        byte[] testData = createTestPcmData(100);
        
        // Should create directory structure automatically
        AudioUtils.writePcmToWav(testData, nestedPath);
        
        assertTrue(Files.exists(nestedPath.getParent()));
        assertTrue(Files.exists(nestedPath));
    }
    
    /**
     * Creates test PCM data with a simple pattern.
     */
    private byte[] createTestPcmData(int numBytes) {
        byte[] data = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }
}
