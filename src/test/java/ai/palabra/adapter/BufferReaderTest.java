package ai.palabra.adapter;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BufferReader Tests")
class BufferReaderTest {
    
    private BufferReader bufferReader;
    
    @BeforeEach
    void setUp() {
        bufferReader = new BufferReader();
    }
    
    @Test
    @DisplayName("Should create BufferReader with default settings")
    void testDefaultConstructor() {
        assertNotNull(bufferReader);
        assertTrue(bufferReader.isReady());
        assertTrue(bufferReader.isEmpty());
        assertEquals(0, bufferReader.getBufferSize());
    }
    
    @Test
    @DisplayName("Should create BufferReader with custom settings")
    void testCustomConstructor() {
        BufferReader customReader = new BufferReader(10, 2000);
        assertNotNull(customReader);
        assertTrue(customReader.isReady());
        assertEquals(2000, customReader.getTimeoutMs());
    }
    
    @Test
    @DisplayName("Should add data to buffer successfully")
    void testAddData() throws PalabraException {
        byte[] testData = "test audio data".getBytes();
        
        bufferReader.addData(testData);
        
        assertEquals(1, bufferReader.getBufferSize());
        assertFalse(bufferReader.isEmpty());
    }
    
    @Test
    @DisplayName("Should ignore null or empty data")
    void testAddNullOrEmptyData() throws PalabraException {
        bufferReader.addData(null);
        bufferReader.addData(new byte[0]);
        
        assertEquals(0, bufferReader.getBufferSize());
        assertTrue(bufferReader.isEmpty());
    }
    
    @Test
    @DisplayName("Should set multiple data chunks")
    void testSetData() throws PalabraException {
        byte[] chunk1 = "chunk1".getBytes();
        byte[] chunk2 = "chunk2".getBytes();
        byte[] chunk3 = "chunk3".getBytes();
        
        bufferReader.setData(chunk1, chunk2, chunk3);
        
        assertEquals(3, bufferReader.getBufferSize());
        assertFalse(bufferReader.isEmpty());
    }
    
    @Test
    @DisplayName("Should read data in order")
    void testReadData() throws PalabraException {
        byte[] chunk1 = "chunk1".getBytes();
        byte[] chunk2 = "chunk2".getBytes();
        
        bufferReader.addData(chunk1);
        bufferReader.addData(chunk2);
        
        byte[] read1 = bufferReader.read();
        byte[] read2 = bufferReader.read();
        
        assertArrayEquals(chunk1, read1);
        assertArrayEquals(chunk2, read2);
    }
    
    @Test
    @DisplayName("Should handle partial reads from large chunks")
    void testPartialReads() throws PalabraException {
        byte[] largeChunk = "this is a large chunk of audio data".getBytes();
        bufferReader.addData(largeChunk);
        
        byte[] part1 = bufferReader.read(10);
        byte[] part2 = bufferReader.read(10);
        
        assertEquals(10, part1.length);
        assertEquals(10, part2.length);
        
        // Verify the data is correct
        assertEquals("this is a ", new String(part1));
        assertEquals("large chun", new String(part2));
    }
    
    @Test
    @DisplayName("Should return null when no data available")
    void testReadWithNoData() throws PalabraException {
        // Set a very short timeout for testing
        bufferReader.setTimeoutMs(100);
        
        byte[] result = bufferReader.read();
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Should clear buffer successfully")
    void testClear() throws PalabraException {
        bufferReader.addData("test data".getBytes());
        assertEquals(1, bufferReader.getBufferSize());
        
        bufferReader.clear();
        
        assertEquals(0, bufferReader.getBufferSize());
        assertTrue(bufferReader.isEmpty());
    }
    
    @Test
    @DisplayName("Should handle end of stream")
    void testEndOfStream() throws PalabraException {
        bufferReader.addData("test data".getBytes());
        assertTrue(bufferReader.isReady());
        
        bufferReader.endOfStream();
        
        assertFalse(bufferReader.isReady());
    }
    
    @Test
    @DisplayName("Should close properly")
    void testClose() throws PalabraException {
        bufferReader.addData("test data".getBytes());
        assertTrue(bufferReader.isReady());
        
        bufferReader.close();
        
        assertFalse(bufferReader.isReady());
        assertTrue(bufferReader.isEmpty());
    }
    
    @Test
    @DisplayName("Should throw exception when adding data to closed reader")
    void testAddDataToClosed() throws PalabraException {
        bufferReader.close();
        
        assertThrows(PalabraException.class, () -> {
            bufferReader.addData("test data".getBytes());
        });
    }
    
    @Test
    @DisplayName("Should throw exception when reading from closed reader")
    void testReadFromClosed() throws PalabraException {
        bufferReader.close();
        
        assertThrows(PalabraException.class, () -> {
            bufferReader.read();
        });
    }
    
    @Test
    @DisplayName("Should throw exception for invalid read size")
    void testInvalidReadSize() {
        assertThrows(PalabraException.class, () -> {
            bufferReader.read(0);
        });
        
        assertThrows(PalabraException.class, () -> {
            bufferReader.read(-1);
        });
    }
    
    @Test
    @DisplayName("Should handle buffer size limits")
    void testBufferSizeLimit() {
        BufferReader limitedReader = new BufferReader(2, 100);
        
        assertDoesNotThrow(() -> {
            limitedReader.addData("chunk1".getBytes());
            limitedReader.addData("chunk2".getBytes());
        });
        
        // This should timeout since buffer is full
        assertThrows(PalabraException.class, () -> {
            limitedReader.addData("chunk3".getBytes());
        });
    }
    
    @Test
    @DisplayName("Should handle timeout configuration")
    void testTimeoutConfiguration() {
        bufferReader.setTimeoutMs(500);
        assertEquals(500, bufferReader.getTimeoutMs());
    }
}
