package ai.palabra.adapter;

import ai.palabra.PalabraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Buffer Adapter Integration Tests")
class BufferAdapterIntegrationTest {
    
    private BufferReader bufferReader;
    private BufferWriter bufferWriter;
    
    @BeforeEach
    void setUp() {
        bufferReader = new BufferReader();
        bufferWriter = new BufferWriter();
    }
    
    @Test
    @DisplayName("Should transfer data from writer to reader")
    void testWriterToReaderTransfer() throws PalabraException {
        // Write some test data
        String[] testMessages = {"Hello", "World", "From", "Buffer", "Test"};
        
        for (String message : testMessages) {
            bufferWriter.write(message.getBytes());
        }
        bufferWriter.flush();
        
        // Transfer all data to reader
        List<byte[]> chunks = bufferWriter.getChunks();
        for (byte[] chunk : chunks) {
            bufferReader.addData(chunk);
        }
        
        // Read and verify data
        byte[] concatenatedData = bufferReader.read(1000); // Read all at once
        String result = new String(concatenatedData);
        
        assertEquals("HelloWorldFromBufferTest", result);
    }
    
    @Test
    @DisplayName("Should handle streaming data transfer")
    void testStreamingTransfer() throws PalabraException {
        BufferWriter autoFlushWriter = new BufferWriter(10, true, 5); // Auto-flush every 5 bytes
        
        // Simulate streaming scenario
        String[] streamChunks = {"chunk", "data1", "data2", "final"};
        
        for (String chunk : streamChunks) {
            autoFlushWriter.write(chunk.getBytes());
        }
        
        // Each write should have triggered a flush (5+ bytes each)
        assertTrue(autoFlushWriter.getChunkCount() >= streamChunks.length);
        
        // Transfer to reader chunk by chunk
        List<byte[]> chunks = autoFlushWriter.getChunks();
        for (byte[] chunk : chunks) {
            bufferReader.addData(chunk);
        }
        
        // Read back the data
        StringBuilder result = new StringBuilder();
        byte[] data;
        while ((data = bufferReader.read()) != null) {
            result.append(new String(data));
        }
        
        assertEquals("chunkdata1data2final", result.toString());
    }
    
    @Test
    @DisplayName("Should handle large data transfer with chunking")
    void testLargeDataTransfer() throws PalabraException {
        // Create a large data set
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("This is line ").append(i).append(" of test data. ");
        }
        byte[] largeDataBytes = largeData.toString().getBytes();
        
        // Write large data
        bufferWriter.write(largeDataBytes);
        bufferWriter.flush();
        
        // Transfer to reader
        byte[] writerData = bufferWriter.getData();
        bufferReader.addData(writerData);
        
        // Read back in chunks
        byte[] readData = bufferReader.read(largeDataBytes.length);
        
        assertArrayEquals(largeDataBytes, readData);
    }
    
    @Test
    @DisplayName("Should handle concurrent read/write operations")
    void testConcurrentOperations() throws Exception {
        final int numOperations = 100;
        final String testData = "concurrent test data ";
        
        // Writer thread
        Thread writerThread = new Thread(() -> {
            try {
                for (int i = 0; i < numOperations; i++) {
                    String data = testData + i;
                    bufferWriter.write(data.getBytes());
                    if (i % 10 == 0) {
                        bufferWriter.flush();
                    }
                    Thread.sleep(1); // Small delay to allow interleaving
                }
                bufferWriter.flush(); // Final flush
            } catch (Exception e) {
                fail("Writer thread failed: " + e.getMessage());
            }
        });
        
        // Reader setup thread (transfers data from writer to reader)
        Thread transferThread = new Thread(() -> {
            try {
                Thread.sleep(50); // Let writer get ahead
                
                while (writerThread.isAlive() || bufferWriter.getChunkCount() > 0) {
                    List<byte[]> chunks = bufferWriter.getChunks();
                    if (!chunks.isEmpty()) {
                        for (byte[] chunk : chunks) {
                            bufferReader.addData(chunk);
                        }
                        bufferWriter.clear();
                    }
                    Thread.sleep(10);
                }
                
                // Final transfer
                byte[] remainingData = bufferWriter.getData();
                if (remainingData.length > 0) {
                    bufferReader.addData(remainingData);
                }
                
            } catch (Exception e) {
                fail("Transfer thread failed: " + e.getMessage());
            }
        });
        
        // Start operations
        writerThread.start();
        transferThread.start();
        
        // Wait for completion
        writerThread.join();
        transferThread.join();
        
        // Verify some data was transferred
        assertFalse(bufferReader.isEmpty());
        
        // Read some data to verify it's accessible
        byte[] readData = bufferReader.read();
        assertNotNull(readData);
        assertTrue(readData.length > 0);
    }
    
    @Test
    @DisplayName("Should handle buffer-to-buffer data pipeline")
    void testBufferPipeline() throws PalabraException {
        // Create a pipeline: Writer1 -> Reader1 -> Writer2 -> Reader2
        BufferWriter writer1 = new BufferWriter();
        BufferReader reader1 = new BufferReader();
        BufferWriter writer2 = new BufferWriter();
        BufferReader reader2 = new BufferReader();
        
        // Stage 1: Write initial data
        String[] messages = {"pipeline", "test", "data", "flow"};
        for (String message : messages) {
            writer1.write(message.getBytes());
        }
        writer1.flush();
        
        // Stage 2: Transfer to first reader
        List<byte[]> chunks1 = writer1.getChunks();
        for (byte[] chunk : chunks1) {
            reader1.addData(chunk);
        }
        
        // Stage 3: Process through first reader to second writer
        byte[] data;
        while ((data = reader1.read()) != null) {
            // Simulate some processing (add prefix)
            String processed = "processed:" + new String(data);
            writer2.write(processed.getBytes());
        }
        writer2.flush();
        
        // Stage 4: Transfer to final reader
        List<byte[]> chunks2 = writer2.getChunks();
        for (byte[] chunk : chunks2) {
            reader2.addData(chunk);
        }
        
        // Stage 5: Verify final result
        byte[] finalData = reader2.read(1000);
        String result = new String(finalData);
        
        assertTrue(result.contains("processed:"));
        assertTrue(result.contains("pipeline"));
        assertTrue(result.contains("test"));
        assertTrue(result.contains("data"));
        assertTrue(result.contains("flow"));
    }
    
    @Test
    @DisplayName("Should handle error conditions gracefully")
    void testErrorConditions() throws PalabraException {
        // Test writing to closed writer
        bufferWriter.close();
        assertThrows(PalabraException.class, () -> {
            bufferWriter.write("test".getBytes());
        });
        
        // Test reading from closed reader
        bufferReader.close();
        assertThrows(PalabraException.class, () -> {
            bufferReader.read();
        });
        
        // Test buffer overflow
        BufferWriter limitedWriter = new BufferWriter(1); // Only 1 chunk allowed
        limitedWriter.write("chunk1".getBytes());
        limitedWriter.flush();
        limitedWriter.write("chunk2".getBytes());
        assertThrows(PalabraException.class, () -> {
            limitedWriter.flush(); // Should fail - buffer full
        });
    }
    
    @Test
    @DisplayName("Should handle resource cleanup in try-with-resources")
    void testResourceCleanup() throws PalabraException {
        byte[] testData = "resource test data".getBytes();
        
        // Test automatic cleanup with try-with-resources
        try (BufferWriter writer = new BufferWriter();
             BufferReader reader = new BufferReader()) {
            
            writer.write(testData);
            writer.flush();
            
            List<byte[]> chunks = writer.getChunks();
            for (byte[] chunk : chunks) {
                reader.addData(chunk);
            }
            
            byte[] readData = reader.read();
            assertArrayEquals(testData, readData);
            
            // Both should be ready inside the try block
            assertTrue(writer.isReady());
            assertTrue(reader.isReady());
        }
        
        // After the try block, resources should be closed automatically
        // We can't test this directly since writer/reader are local variables,
        // but the close() methods should have been called
    }
}
