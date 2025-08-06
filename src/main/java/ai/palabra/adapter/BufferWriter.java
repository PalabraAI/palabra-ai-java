package ai.palabra.adapter;

import ai.palabra.Writer;
import ai.palabra.PalabraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory buffer-based audio output adapter.
 * Collects audio data in memory buffers, useful for testing, processing, or 
 * when audio data needs to be captured for further manipulation.
 */
public class BufferWriter extends Writer {
    private static final Logger logger = LoggerFactory.getLogger(BufferWriter.class);
    
    private final List<byte[]> audioChunks;
    private final ByteArrayOutputStream currentBuffer;
    private final AtomicBoolean isReady;
    private final AtomicBoolean isClosed;
    private final Object lock;
    private int maxBufferSize;
    private boolean autoFlush;
    private int flushThreshold;
    
    /**
     * Create a new BufferWriter with default settings.
     */
    public BufferWriter() {
        this(Integer.MAX_VALUE);
    }
    
    /**
     * Create a new BufferWriter with specified maximum buffer size.
     * 
     * @param maxBufferSize Maximum number of chunks to store
     */
    public BufferWriter(int maxBufferSize) {
        this(maxBufferSize, false, 4096);
    }
    
    /**
     * Create a new BufferWriter with specified settings.
     * 
     * @param maxBufferSize Maximum number of chunks to store
     * @param autoFlush Whether to automatically flush when threshold is reached
     * @param flushThreshold Buffer size threshold for auto-flush (in bytes)
     */
    public BufferWriter(int maxBufferSize, boolean autoFlush, int flushThreshold) {
        this.audioChunks = Collections.synchronizedList(new ArrayList<>());
        this.currentBuffer = new ByteArrayOutputStream();
        this.isReady = new AtomicBoolean(true);
        this.isClosed = new AtomicBoolean(false);
        this.lock = new Object();
        this.maxBufferSize = maxBufferSize;
        this.autoFlush = autoFlush;
        this.flushThreshold = flushThreshold;
        
        logger.debug("Created BufferWriter with maxBufferSize={}, autoFlush={}, flushThreshold={}", 
                    maxBufferSize, autoFlush, flushThreshold);
    }
    
    /**
     * Write audio data to the buffer.
     * 
     * @param data Audio data to write
     * @throws PalabraException If writing fails or writer is closed
     */
    @Override
    public void write(byte[] data) throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("BufferWriter is closed");
        }
        
        if (data == null || data.length == 0) {
            logger.debug("Ignoring null or empty data");
            return;
        }
        
        synchronized (lock) {
            try {
                currentBuffer.write(data);
                logger.debug("Wrote {} bytes to buffer, current size: {}", data.length, currentBuffer.size());
                
                // Check if we should auto-flush
                if (autoFlush && currentBuffer.size() >= flushThreshold) {
                    flushInternal();
                }
                
            } catch (IOException e) {
                throw new PalabraException("Failed to write data to buffer", e);
            }
        }
    }
    
    /**
     * Flush the current buffer content to the chunks list.
     * This moves data from the write buffer to the stored chunks.
     * 
     * @throws PalabraException If flushing fails
     */
    public void flush() throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("BufferWriter is closed");
        }
        
        synchronized (lock) {
            flushInternal();
        }
    }
    
    /**
     * Internal flush implementation (must be called within synchronized block).
     */
    private void flushInternal() throws PalabraException {
        if (currentBuffer.size() > 0) {
            if (audioChunks.size() >= maxBufferSize) {
                throw new PalabraException("Buffer capacity exceeded, cannot flush more data");
            }
            
            byte[] chunk = currentBuffer.toByteArray();
            audioChunks.add(chunk);
            currentBuffer.reset();
            
            logger.debug("Flushed {} bytes to chunks, total chunks: {}", chunk.length, audioChunks.size());
        }
    }
    
    /**
     * Get all audio chunks written so far.
     * This returns a copy of the chunks list to prevent external modification.
     * 
     * @return List of audio data chunks
     */
    public List<byte[]> getChunks() {
        synchronized (lock) {
            return new ArrayList<>(audioChunks);
        }
    }
    
    /**
     * Get all audio data as a single byte array.
     * This concatenates all chunks including any unflushed data in the current buffer.
     * 
     * @return Complete audio data
     */
    public byte[] getData() {
        synchronized (lock) {
            try {
                ByteArrayOutputStream allData = new ByteArrayOutputStream();
                
                // Add all flushed chunks
                for (byte[] chunk : audioChunks) {
                    allData.write(chunk);
                }
                
                // Add current buffer content (unflushed data)
                if (currentBuffer.size() > 0) {
                    allData.write(currentBuffer.toByteArray());
                }
                
                byte[] result = allData.toByteArray();
                logger.debug("Retrieved {} total bytes from {} chunks plus {} buffered bytes", 
                            result.length, audioChunks.size(), currentBuffer.size());
                
                return result;
                
            } catch (IOException e) {
                logger.error("Failed to concatenate audio data", e);
                return new byte[0];
            }
        }
    }
    
    /**
     * Get the number of chunks currently stored.
     * This does not include unflushed data in the current buffer.
     * 
     * @return Number of chunks
     */
    public int getChunkCount() {
        return audioChunks.size();
    }
    
    /**
     * Get the total size of all stored data.
     * This includes both flushed chunks and unflushed buffer data.
     * 
     * @return Total size in bytes
     */
    public int getTotalSize() {
        synchronized (lock) {
            int total = audioChunks.stream().mapToInt(chunk -> chunk.length).sum();
            total += currentBuffer.size();
            return total;
        }
    }
    
    /**
     * Get the size of unflushed data in the current buffer.
     * 
     * @return Buffer size in bytes
     */
    public int getBufferSize() {
        synchronized (lock) {
            return currentBuffer.size();
        }
    }
    
    /**
     * Clear all stored data.
     * This removes all chunks and clears the current buffer.
     */
    public void clear() {
        synchronized (lock) {
            audioChunks.clear();
            currentBuffer.reset();
            logger.debug("Cleared all data");
        }
    }
    
    /**
     * Check if the writer is ready to write data.
     * 
     * @return true if ready to write, false otherwise
     */
    @Override
    public boolean isReady() {
        return isReady.get() && !isClosed.get();
    }
    
    /**
     * Check if auto-flush is enabled.
     * 
     * @return true if auto-flush is enabled, false otherwise
     */
    public boolean isAutoFlush() {
        return autoFlush;
    }
    
    /**
     * Set auto-flush behavior.
     * 
     * @param autoFlush Whether to enable auto-flush
     */
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }
    
    /**
     * Get the auto-flush threshold.
     * 
     * @return Threshold in bytes
     */
    public int getFlushThreshold() {
        return flushThreshold;
    }
    
    /**
     * Set the auto-flush threshold.
     * 
     * @param flushThreshold Threshold in bytes
     */
    public void setFlushThreshold(int flushThreshold) {
        this.flushThreshold = flushThreshold;
    }
    
    /**
     * Get the maximum buffer size.
     * 
     * @return Maximum number of chunks
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }
    
    /**
     * Set the maximum buffer size.
     * 
     * @param maxBufferSize Maximum number of chunks
     */
    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }
    
    /**
     * Close the writer and release resources.
     * This automatically flushes any remaining data.
     * 
     * @throws PalabraException If closing fails
     */
    @Override
    public void close() throws PalabraException {
        if (isClosed.compareAndSet(false, true)) {
            synchronized (lock) {
                try {
                    // Flush any remaining data
                    flushInternal();
                    isReady.set(false);
                    logger.debug("BufferWriter closed with {} total chunks", audioChunks.size());
                    
                } catch (Exception e) {
                    logger.error("Error during BufferWriter close", e);
                    throw new PalabraException("Failed to close BufferWriter", e);
                }
            }
        }
    }
}
