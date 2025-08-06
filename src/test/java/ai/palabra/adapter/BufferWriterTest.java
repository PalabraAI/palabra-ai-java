package ai.palabra.adapter;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BufferWriter Tests")
class BufferWriterTest {
    
    private BufferWriter bufferWriter;
    
    @BeforeEach
    void setUp() {
        bufferWriter = new BufferWriter();
    }
    
    @Test
    @DisplayName("Should create BufferWriter with default settings")
    void testDefaultConstructor() {
        assertNotNull(bufferWriter);
        assertTrue(bufferWriter.isReady());
        assertEquals(0, bufferWriter.getChunkCount());
        assertEquals(0, bufferWriter.getTotalSize());
        assertEquals(0, bufferWriter.getBufferSize());
    }
    
    @Test
    @DisplayName("Should create BufferWriter with custom settings")
    void testCustomConstructor() {
        BufferWriter customWriter = new BufferWriter(10, true, 2048);
        assertNotNull(customWriter);
        assertTrue(customWriter.isReady());
        assertTrue(customWriter.isAutoFlush());
        assertEquals(2048, customWriter.getFlushThreshold());
        assertEquals(10, customWriter.getMaxBufferSize());
    }
    
    @Test
    @DisplayName("Should write data to buffer")
    void testWriteData() throws PalabraException {
        byte[] testData = "test audio data".getBytes();
        
        bufferWriter.write(testData);
        
        assertEquals(testData.length, bufferWriter.getBufferSize());
        assertEquals(testData.length, bufferWriter.getTotalSize());
        assertEquals(0, bufferWriter.getChunkCount()); // Not flushed yet
    }
    
    @Test
    @DisplayName("Should ignore null or empty data")
    void testWriteNullOrEmptyData() throws PalabraException {
        bufferWriter.write(null);
        bufferWriter.write(new byte[0]);
        
        assertEquals(0, bufferWriter.getBufferSize());
        assertEquals(0, bufferWriter.getTotalSize());
    }
    
    @Test
    @DisplayName("Should flush data to chunks")
    void testFlushData() throws PalabraException {
        byte[] testData = "test audio data".getBytes();
        
        bufferWriter.write(testData);
        bufferWriter.flush();
        
        assertEquals(1, bufferWriter.getChunkCount());
        assertEquals(0, bufferWriter.getBufferSize()); // Buffer cleared after flush
        assertEquals(testData.length, bufferWriter.getTotalSize());
        
        List<byte[]> chunks = bufferWriter.getChunks();
        assertEquals(1, chunks.size());
        assertArrayEquals(testData, chunks.get(0));
    }
    
    @Test
    @DisplayName("Should auto-flush when threshold reached")
    void testAutoFlush() throws PalabraException {
        BufferWriter autoFlushWriter = new BufferWriter(10, true, 10);
        
        // Write data that exceeds threshold
        autoFlushWriter.write("small data".getBytes()); // 10 bytes, should trigger flush
        
        assertEquals(1, autoFlushWriter.getChunkCount());
        assertEquals(0, autoFlushWriter.getBufferSize());
    }
    
    @Test
    @DisplayName("Should get all data as single array")
    void testGetData() throws PalabraException {
        byte[] chunk1 = "chunk1".getBytes();
        byte[] chunk2 = "chunk2".getBytes();
        byte[] unflushed = "unflushed".getBytes();
        
        bufferWriter.write(chunk1);
        bufferWriter.flush();
        bufferWriter.write(chunk2);
        bufferWriter.flush();
        bufferWriter.write(unflushed); // Not flushed
        
        byte[] allData = bufferWriter.getData();
        String expected = "chunk1chunk2unflushed";
        
        assertEquals(expected, new String(allData));
        assertEquals(expected.length(), allData.length);
    }
    
    @Test
    @DisplayName("Should handle multiple writes before flush")
    void testMultipleWrites() throws PalabraException {
        bufferWriter.write("part1".getBytes());
        bufferWriter.write("part2".getBytes());
        bufferWriter.write("part3".getBytes());
        
        assertEquals(15, bufferWriter.getBufferSize()); // 5 + 5 + 5
        assertEquals(0, bufferWriter.getChunkCount());
        
        bufferWriter.flush();
        
        assertEquals(1, bufferWriter.getChunkCount());
        assertEquals(0, bufferWriter.getBufferSize());
        
        List<byte[]> chunks = bufferWriter.getChunks();
        assertEquals("part1part2part3", new String(chunks.get(0)));
    }
    
    @Test
    @DisplayName("Should clear all data")
    void testClear() throws PalabraException {
        bufferWriter.write("data1".getBytes());
        bufferWriter.flush();
        bufferWriter.write("data2".getBytes());
        
        assertEquals(1, bufferWriter.getChunkCount());
        assertEquals(5, bufferWriter.getBufferSize());
        
        bufferWriter.clear();
        
        assertEquals(0, bufferWriter.getChunkCount());
        assertEquals(0, bufferWriter.getBufferSize());
        assertEquals(0, bufferWriter.getTotalSize());
    }
    
    @Test
    @DisplayName("Should handle buffer size limit")
    void testBufferSizeLimit() throws PalabraException {
        BufferWriter limitedWriter = new BufferWriter(2); // Max 2 chunks
        
        limitedWriter.write("chunk1".getBytes());
        limitedWriter.flush();
        limitedWriter.write("chunk2".getBytes());
        limitedWriter.flush();
        
        // This should fail because we've reached the limit
        limitedWriter.write("chunk3".getBytes());
        assertThrows(PalabraException.class, () -> {
            limitedWriter.flush();
        });
    }
    
    @Test
    @DisplayName("Should close properly and flush remaining data")
    void testClose() throws PalabraException {
        bufferWriter.write("unflushed data".getBytes());
        assertTrue(bufferWriter.isReady());
        assertEquals(0, bufferWriter.getChunkCount());
        
        bufferWriter.close();
        
        assertFalse(bufferWriter.isReady());
        assertEquals(1, bufferWriter.getChunkCount()); // Data was flushed on close
    }
    
    @Test
    @DisplayName("Should throw exception when writing to closed writer")
    void testWriteToClosed() throws PalabraException {
        bufferWriter.close();
        
        assertThrows(PalabraException.class, () -> {
            bufferWriter.write("test data".getBytes());
        });
    }
    
    @Test
    @DisplayName("Should throw exception when flushing closed writer")
    void testFlushClosed() throws PalabraException {
        bufferWriter.close();
        
        assertThrows(PalabraException.class, () -> {
            bufferWriter.flush();
        });
    }
    
    @Test
    @DisplayName("Should handle configuration changes")
    void testConfigurationChanges() {
        bufferWriter.setAutoFlush(true);
        bufferWriter.setFlushThreshold(512);
        bufferWriter.setMaxBufferSize(20);
        
        assertTrue(bufferWriter.isAutoFlush());
        assertEquals(512, bufferWriter.getFlushThreshold());
        assertEquals(20, bufferWriter.getMaxBufferSize());
    }
    
    @Test
    @DisplayName("Should maintain thread safety")
    void testThreadSafety() throws Exception {
        final int numThreads = 5;
        final int writesPerThread = 10;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < writesPerThread; j++) {
                        String data = "thread" + threadId + "_write" + j;
                        bufferWriter.write(data.getBytes());
                    }
                } catch (PalabraException e) {
                    fail("Thread " + threadId + " failed: " + e.getMessage());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify we received all the data
        int expectedTotalSize = numThreads * writesPerThread * "thread0_write0".length();
        assertTrue(bufferWriter.getTotalSize() >= expectedTotalSize);
    }
}
