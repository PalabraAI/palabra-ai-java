package ai.palabra.adapter;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileWriter class.
 */
class FileWriterTest {

    @TempDir
    Path tempDir;
    
    private Path outputFile;
    private FileWriter fileWriter;
    
    @BeforeEach
    void setUp() {
        outputFile = tempDir.resolve("output_test.wav");
    }
    
    @AfterEach
    void tearDown() throws PalabraException {
        if (fileWriter != null && !fileWriter.isClosed()) {
            fileWriter.close();
        }
    }
    
    @Test
    void testConstructor() {
        fileWriter = new FileWriter(outputFile.toString());
        
        assertEquals(outputFile.toString(), fileWriter.getFilePath());
        assertTrue(fileWriter.isReady());
        assertFalse(fileWriter.isClosed());
        assertEquals(0, fileWriter.getBufferedSize());
    }
    
    @Test
    void testWriteAndClose() throws PalabraException, IOException {
        fileWriter = new FileWriter(outputFile.toString());
        
        // Write some test PCM data
        byte[] testData = createTestPcmData(1024);
        fileWriter.write(testData);
        
        assertEquals(testData.length, fileWriter.getBufferedSize());
        assertTrue(fileWriter.isReady());
        
        // Close to save the file
        fileWriter.close();
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        assertFalse(fileWriter.isReady());
        assertTrue(fileWriter.isClosed());
    }
    
    @Test
    void testMultipleWrites() throws PalabraException, IOException {
        fileWriter = new FileWriter(outputFile.toString());
        
        byte[] chunk1 = createTestPcmData(512);
        byte[] chunk2 = createTestPcmData(768);
        
        fileWriter.write(chunk1);
        assertEquals(chunk1.length, fileWriter.getBufferedSize());
        
        fileWriter.write(chunk2);
        assertEquals(chunk1.length + chunk2.length, fileWriter.getBufferedSize());
        
        fileWriter.close();
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > chunk1.length + chunk2.length); // WAV header adds size
    }
    
    @Test
    void testWriteNullData() throws PalabraException {
        fileWriter = new FileWriter(outputFile.toString());
        
        // Should not throw exception, just ignore
        fileWriter.write(null);
        assertEquals(0, fileWriter.getBufferedSize());
        
        fileWriter.close();
    }
    
    @Test
    void testWriteEmptyData() throws PalabraException {
        fileWriter = new FileWriter(outputFile.toString());
        
        // Should not throw exception, just ignore
        fileWriter.write(new byte[0]);
        assertEquals(0, fileWriter.getBufferedSize());
        
        fileWriter.close();
    }
    
    @Test
    void testWriteAfterClose() throws PalabraException {
        fileWriter = new FileWriter(outputFile.toString());
        
        fileWriter.close();
        assertTrue(fileWriter.isClosed());
        
        PalabraException exception = assertThrows(PalabraException.class, () -> {
            fileWriter.write(createTestPcmData(100));
        });
        
        assertTrue(exception.getMessage().contains("Cannot write to closed FileWriter"));
    }
    
    @Test
    void testMultipleCloses() throws PalabraException {
        fileWriter = new FileWriter(outputFile.toString());
        
        fileWriter.close();
        assertTrue(fileWriter.isClosed());
        
        // Second close should not throw exception
        assertDoesNotThrow(() -> fileWriter.close());
    }
    
    @Test
    void testFlush() throws PalabraException, IOException {
        fileWriter = new FileWriter(outputFile.toString());
        
        byte[] testData = createTestPcmData(512);
        fileWriter.write(testData);
        
        // Flush should write current data to file
        fileWriter.flush();
        
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        assertTrue(fileWriter.isReady()); // Should still be ready for more data
        assertFalse(fileWriter.isClosed());
    }
    
    @Test
    void testFlushAfterClose() throws PalabraException {
        fileWriter = new FileWriter(outputFile.toString());
        
        fileWriter.close();
        
        PalabraException exception = assertThrows(PalabraException.class, () -> {
            fileWriter.flush();
        });
        
        assertTrue(exception.getMessage().contains("Cannot flush closed FileWriter"));
    }
    
    @Test
    void testCreateDirectoryIfNotExists() throws PalabraException, IOException {
        Path nestedDir = tempDir.resolve("nested").resolve("directory");
        Path nestedFile = nestedDir.resolve("test.wav");
        
        assertFalse(Files.exists(nestedDir));
        
        fileWriter = new FileWriter(nestedFile.toString());
        fileWriter.write(createTestPcmData(100));
        fileWriter.close();
        
        assertTrue(Files.exists(nestedDir));
        assertTrue(Files.exists(nestedFile));
    }
    
    @Test
    void testWriteEmptyFileOnClose() throws PalabraException, IOException {
        fileWriter = new FileWriter(outputFile.toString());
        
        // Close without writing any data
        fileWriter.close();
        
        assertTrue(Files.exists(outputFile));
        // Should still create a valid (empty) WAV file
        assertTrue(Files.size(outputFile) > 0); // WAV header is present
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
