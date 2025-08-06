package ai.palabra.adapter;

import ai.palabra.PalabraException;
import ai.palabra.util.AudioUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FileReader and FileWriter working together.
 * These tests demonstrate complete file-to-file audio processing workflows.
 */
class FileAdapterIntegrationTest {

    @TempDir
    Path tempDir;
    
    @Test
    void testFileToFileProcessing() throws IOException, PalabraException {
        // Setup: Create an input audio file
        Path inputFile = tempDir.resolve("input.wav");
        Path outputFile = tempDir.resolve("output.wav");
        
        // Create test audio data (2 seconds of simple tone)
        byte[] originalAudioData = createTestAudioData(96000); // 2 seconds at 48kHz
        AudioUtils.writePcmToWav(originalAudioData, inputFile);
        
        assertTrue(Files.exists(inputFile));
        assertTrue(Files.size(inputFile) > originalAudioData.length);
        
        // Test: Process file through FileReader -> FileWriter pipeline
        try (FileReader reader = new FileReader(inputFile.toString());
             FileWriter writer = new FileWriter(outputFile.toString())) {
            
            // Initialize reader
            reader.initialize();
            assertTrue(reader.isReady());
            
            // Process audio in chunks
            byte[] chunk;
            int totalBytesProcessed = 0;
            
            while ((chunk = reader.read(4096)) != null) {
                writer.write(chunk);
                totalBytesProcessed += chunk.length;
            }
            
            assertTrue(totalBytesProcessed > 0);
            assertEquals(totalBytesProcessed, reader.getTotalSize());
            assertTrue(reader.isEndOfFile());
            
            // Close writer to finalize output file
            writer.close();
        }
        
        // Verify: Output file should exist and have reasonable size
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        
        // The output file should be similar in size to input (WAV headers may differ slightly)
        long inputSize = Files.size(inputFile);
        long outputSize = Files.size(outputFile);
        assertTrue(Math.abs(inputSize - outputSize) < 1000, 
                   "Input size: " + inputSize + ", Output size: " + outputSize);
    }
    
    @Test
    void testMultipleReadWriteCycles() throws IOException, PalabraException {
        Path sourceFile = tempDir.resolve("source.wav");
        byte[] testData = createTestAudioData(48000); // 1 second
        AudioUtils.writePcmToWav(testData, sourceFile);
        
        // Process through multiple read-write cycles
        Path[] files = new Path[4];
        files[0] = sourceFile;
        
        for (int i = 1; i < files.length; i++) {
            files[i] = tempDir.resolve("cycle_" + i + ".wav");
            
            try (FileReader reader = new FileReader(files[i-1].toString());
                 FileWriter writer = new FileWriter(files[i].toString())) {
                
                reader.initialize();
                
                byte[] chunk;
                while ((chunk = reader.read(2048)) != null) {
                    writer.write(chunk);
                }
                
                writer.close();
            }
            
            assertTrue(Files.exists(files[i]));
            assertTrue(Files.size(files[i]) > 0);
        }
        
        // All files should be roughly the same size
        long originalSize = Files.size(files[0]);
        for (int i = 1; i < files.length; i++) {
            long size = Files.size(files[i]);
            assertTrue(Math.abs(originalSize - size) < 500,
                       "File " + i + " size differs too much: " + size + " vs " + originalSize);
        }
    }
    
    @Test
    void testReaderResetAndReprocess() throws IOException, PalabraException {
        Path inputFile = tempDir.resolve("input.wav");
        Path output1 = tempDir.resolve("output1.wav");
        Path output2 = tempDir.resolve("output2.wav");
        
        byte[] testData = createTestAudioData(24000); // 0.5 seconds
        AudioUtils.writePcmToWav(testData, inputFile);
        
        try (FileReader reader = new FileReader(inputFile.toString())) {
            reader.initialize();
            
            // First processing pass
            try (FileWriter writer1 = new FileWriter(output1.toString())) {
                byte[] chunk;
                while ((chunk = reader.read(1024)) != null) {
                    writer1.write(chunk);
                }
                writer1.close();
            }
            
            assertTrue(reader.isEndOfFile());
            
            // Reset and process again
            reader.reset();
            assertFalse(reader.isEndOfFile());
            assertEquals(0, reader.getPosition());
            
            try (FileWriter writer2 = new FileWriter(output2.toString())) {
                byte[] chunk;
                while ((chunk = reader.read(1024)) != null) {
                    writer2.write(chunk);
                }
                writer2.close();
            }
        }
        
        // Both output files should exist and be similar
        assertTrue(Files.exists(output1));
        assertTrue(Files.exists(output2));
        
        long size1 = Files.size(output1);
        long size2 = Files.size(output2);
        assertEquals(size1, size2, "Output files should be identical in size");
    }
    
    @Test
    void testEmptyFileHandling() throws IOException, PalabraException {
        Path outputFile = tempDir.resolve("empty_output.wav");
        
        // Write no data and close immediately
        try (FileWriter writer = new FileWriter(outputFile.toString())) {
            writer.close();
        }
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0); // Should have WAV header
        
        // Now try to read the empty file
        try (FileReader reader = new FileReader(outputFile.toString())) {
            reader.initialize();
            assertTrue(reader.isReady());
            
            // Should be able to read (but get minimal data due to WAV header processing)
            byte[] chunk = reader.read();
            // May be null or very small due to header-only file
            
            assertTrue(reader.isEndOfFile() || (chunk != null && chunk.length >= 0));
        }
    }
    
    @Test
    void testLargeFileProcessing() throws IOException, PalabraException {
        Path inputFile = tempDir.resolve("large_input.wav");
        Path outputFile = tempDir.resolve("large_output.wav");
        
        // Create larger test file (10 seconds = 480,000 samples = 960,000 bytes)
        byte[] largeTestData = createTestAudioData(480000);
        AudioUtils.writePcmToWav(largeTestData, inputFile);
        
        try (FileReader reader = new FileReader(inputFile.toString());
             FileWriter writer = new FileWriter(outputFile.toString())) {
            
            reader.initialize();
            
            // Process in smaller chunks to test streaming
            byte[] chunk;
            int chunkCount = 0;
            int totalBytes = 0;
            
            while ((chunk = reader.read(8192)) != null) {
                writer.write(chunk);
                chunkCount++;
                totalBytes += chunk.length;
            }
            
            assertTrue(chunkCount > 10, "Should process multiple chunks: " + chunkCount);
            assertTrue(totalBytes > 900000, "Should process substantial data: " + totalBytes);
            
            writer.close();
        }
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 900000);
    }
    
    /**
     * Creates test PCM audio data with a simple sine wave pattern.
     */
    private byte[] createTestAudioData(int numSamples) {
        byte[] data = new byte[numSamples * 2]; // 16-bit = 2 bytes per sample
        
        // Generate a simple 440Hz sine wave
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * 440.0 * i / 48000.0; // 440Hz at 48kHz sample rate
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE / 4); // Quarter volume
            
            // Little-endian 16-bit
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return data;
    }
}
