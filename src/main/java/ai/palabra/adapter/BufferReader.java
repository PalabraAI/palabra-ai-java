package ai.palabra.adapter;

import ai.palabra.Reader;
import ai.palabra.PalabraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory buffer-based audio input adapter.
 * Provides audio data from byte buffers, useful for streaming scenarios or when audio data
 * is generated programmatically.
 */
public class BufferReader extends Reader {
    private static final Logger logger = LoggerFactory.getLogger(BufferReader.class);
    private static final int DEFAULT_CHUNK_SIZE = 4096;
    private static final int DEFAULT_TIMEOUT_MS = 1000;
    
    private final BlockingQueue<byte[]> audioBuffer;
    private final AtomicBoolean isReady;
    private final AtomicBoolean isClosed;
    private byte[] currentChunk;
    private int currentPosition;
    private int maxBufferSize;
    private long timeoutMs;
    
    /**
     * Create a new BufferReader with default settings.
     */
    public BufferReader() {
        this(Integer.MAX_VALUE);
    }
    
    /**
     * Create a new BufferReader with specified maximum buffer size.
     * 
     * @param maxBufferSize Maximum number of chunks to buffer
     */
    public BufferReader(int maxBufferSize) {
        this(maxBufferSize, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * Create a new BufferReader with specified settings.
     * 
     * @param maxBufferSize Maximum number of chunks to buffer
     * @param timeoutMs Timeout in milliseconds for read operations
     */
    public BufferReader(int maxBufferSize, long timeoutMs) {
        this.audioBuffer = new LinkedBlockingQueue<>(maxBufferSize);
        this.isReady = new AtomicBoolean(true);
        this.isClosed = new AtomicBoolean(false);
        this.maxBufferSize = maxBufferSize;
        this.timeoutMs = timeoutMs;
        this.currentChunk = null;
        this.currentPosition = 0;
        
        logger.debug("Created BufferReader with maxBufferSize={}, timeoutMs={}", maxBufferSize, timeoutMs);
    }
    
    /**
     * Add audio data to the buffer.
     * 
     * @param data Audio data to add
     * @throws PalabraException If buffer is full or reader is closed
     */
    public void addData(byte[] data) throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("BufferReader is closed");
        }
        
        if (data == null || data.length == 0) {
            logger.debug("Ignoring null or empty data");
            return;
        }
        
        try {
            boolean added = audioBuffer.offer(data.clone(), timeoutMs, TimeUnit.MILLISECONDS);
            if (!added) {
                throw new PalabraException("Buffer is full, could not add data within timeout");
            }
            logger.debug("Added {} bytes to buffer, current size: {}", data.length, audioBuffer.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PalabraException("Interrupted while adding data to buffer", e);
        }
    }
    
    /**
     * Set multiple chunks of audio data at once.
     * This clears the existing buffer and adds all provided chunks.
     * 
     * @param chunks Array of audio data chunks
     * @throws PalabraException If operation fails
     */
    public void setData(byte[]... chunks) throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("BufferReader is closed");
        }
        
        audioBuffer.clear();
        currentChunk = null;
        currentPosition = 0;
        
        for (byte[] chunk : chunks) {
            addData(chunk);
        }
        
        logger.debug("Set {} chunks in buffer", chunks.length);
    }
    
    /**
     * Read audio data from the buffer.
     * 
     * @return Audio data chunk, or null if no data available
     * @throws PalabraException If reading fails
     */
    @Override
    public byte[] read() throws PalabraException {
        return read(DEFAULT_CHUNK_SIZE);
    }
    
    /**
     * Read specified amount of audio data from the buffer.
     * 
     * @param size Maximum number of bytes to read
     * @return Audio data chunk, or null if no data available
     * @throws PalabraException If reading fails
     */
    public byte[] read(int size) throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("BufferReader is closed");
        }
        
        if (size <= 0) {
            throw new PalabraException("Read size must be positive");
        }
        
        // If we have a partial chunk from previous read, continue from there
        if (currentChunk != null && currentPosition < currentChunk.length) {
            return readFromCurrentChunk(size);
        }
        
        // Get next chunk from buffer
        try {
            currentChunk = audioBuffer.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (currentChunk == null) {
                logger.debug("No data available in buffer within timeout");
                return null;
            }
            
            currentPosition = 0;
            logger.debug("Retrieved chunk of {} bytes from buffer", currentChunk.length);
            
            return readFromCurrentChunk(size);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PalabraException("Interrupted while reading from buffer", e);
        }
    }
    
    /**
     * Read data from the current chunk, handling partial reads.
     */
    private byte[] readFromCurrentChunk(int size) {
        int availableBytes = currentChunk.length - currentPosition;
        int bytesToRead = Math.min(size, availableBytes);
        
        byte[] result = new byte[bytesToRead];
        System.arraycopy(currentChunk, currentPosition, result, 0, bytesToRead);
        currentPosition += bytesToRead;
        
        logger.debug("Read {} bytes from current chunk, position now {}/{}", 
                    bytesToRead, currentPosition, currentChunk.length);
        
        return result;
    }
    
    /**
     * Check if the reader is ready to read data.
     * 
     * @return true if ready to read, false otherwise
     */
    @Override
    public boolean isReady() {
        return isReady.get() && !isClosed.get();
    }
    
    /**
     * Get the current buffer size (number of chunks waiting).
     * 
     * @return Number of chunks in buffer
     */
    public int getBufferSize() {
        return audioBuffer.size();
    }
    
    /**
     * Check if the buffer is empty.
     * 
     * @return true if buffer is empty, false otherwise
     */
    public boolean isEmpty() {
        return audioBuffer.isEmpty() && (currentChunk == null || currentPosition >= currentChunk.length);
    }
    
    /**
     * Clear all buffered data.
     */
    public void clear() {
        audioBuffer.clear();
        currentChunk = null;
        currentPosition = 0;
        logger.debug("Cleared buffer");
    }
    
    /**
     * Signal end of data stream.
     * This marks the reader as not ready and clears any remaining data.
     */
    public void endOfStream() {
        isReady.set(false);
        logger.debug("End of stream signaled");
    }
    
    /**
     * Close the reader and release resources.
     * 
     * @throws PalabraException If closing fails
     */
    @Override
    public void close() throws PalabraException {
        if (isClosed.compareAndSet(false, true)) {
            clear();
            isReady.set(false);
            logger.debug("BufferReader closed");
        }
    }
    
    /**
     * Get the configured timeout for read operations.
     * 
     * @return Timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    /**
     * Set the timeout for read operations.
     * 
     * @param timeoutMs Timeout in milliseconds
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
