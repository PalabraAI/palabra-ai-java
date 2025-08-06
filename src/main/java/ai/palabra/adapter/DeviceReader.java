package ai.palabra.adapter;

import ai.palabra.Reader;
import ai.palabra.PalabraException;
import ai.palabra.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Device-based audio input adapter for live microphone access.
 * Uses Java Sound API to capture live audio from the system microphone.
 */
public class DeviceReader extends Reader {
    private static final Logger logger = LoggerFactory.getLogger(DeviceReader.class);
    
    // Default configuration matching AudioUtils PCM format
    private static final int DEFAULT_CHUNK_SIZE = 4096;
    private static final int DEFAULT_TIMEOUT_MS = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_QUEUE_CAPACITY = 50; // Maximum chunks in queue
    
    private final int chunkSize;
    private final int timeoutMs;
    private final AudioFormat audioFormat;
    private final BlockingQueue<byte[]> audioQueue;
    private final AtomicBoolean isRecording;
    private final AtomicBoolean isClosed;
    private final int maxQueueCapacity;
    
    private TargetDataLine microphone;
    private Thread captureThread;
    private volatile long totalBytesRead = 0;
    private volatile long droppedChunks = 0;
    
    /**
     * Create a new DeviceReader with default settings.
     * Uses PCM 16-bit mono 48kHz format matching AudioUtils.
     */
    public DeviceReader() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * Create a new DeviceReader with custom chunk size and timeout.
     * 
     * @param chunkSize Size of audio chunks to read (in bytes)
     * @param timeoutMs Timeout for read operations (in milliseconds)
     */
    public DeviceReader(int chunkSize, int timeoutMs) {
        this.chunkSize = chunkSize;
        this.timeoutMs = timeoutMs;
        this.audioFormat = AudioUtils.getPcmFormat();
        this.audioQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.isRecording = new AtomicBoolean(false);
        this.isClosed = new AtomicBoolean(false);
        this.maxQueueCapacity = DEFAULT_QUEUE_CAPACITY;
        
        logger.debug("Created DeviceReader with chunkSize={}, timeoutMs={}", chunkSize, timeoutMs);
    }
    
    /**
     * Create a new DeviceReader with custom audio format.
     * 
     * @param chunkSize Size of audio chunks to read (in bytes)
     * @param timeoutMs Timeout for read operations (in milliseconds)
     * @param audioFormat Custom audio format to use
     */
    public DeviceReader(int chunkSize, int timeoutMs, AudioFormat audioFormat) {
        this.chunkSize = chunkSize;
        this.timeoutMs = timeoutMs;
        this.audioFormat = audioFormat;
        this.audioQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.isRecording = new AtomicBoolean(false);
        this.isClosed = new AtomicBoolean(false);
        this.maxQueueCapacity = DEFAULT_QUEUE_CAPACITY;
        
        logger.debug("Created DeviceReader with custom format: {}", audioFormat);
    }
    
    /**
     * Initialize and start audio capture from the microphone.
     * Must be called before reading audio data.
     * 
     * @throws PalabraException If microphone initialization fails
     */
    public void initialize() throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("Cannot initialize closed DeviceReader");
        }
        
        if (isRecording.get()) {
            logger.debug("DeviceReader already initialized and recording");
            return;
        }
        
        try {
            // Try to find and open microphone with preferred format
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            
            if (!AudioSystem.isLineSupported(info)) {
                // Try to find a compatible format
                AudioFormat compatibleFormat = findCompatibleFormat();
                if (compatibleFormat != null) {
                    logger.info("Using compatible format instead of preferred: {}", compatibleFormat);
                    info = new DataLine.Info(TargetDataLine.class, compatibleFormat);
                } else {
                    throw new PalabraException("No compatible microphone format found");
                }
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(info.getFormats()[0], DEFAULT_BUFFER_SIZE);
            microphone.start();
            
            logger.info("Microphone opened successfully with format: {}", microphone.getFormat());
            
            // Start audio capture thread
            startCaptureThread();
            isRecording.set(true);
            
        } catch (LineUnavailableException e) {
            logger.error("Failed to open microphone", e);
            throw new PalabraException("Failed to open microphone: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find a compatible audio format if the preferred format is not supported.
     * 
     * @return Compatible AudioFormat or null if none found
     */
    private AudioFormat findCompatibleFormat() {
        // Try common audio formats in order of preference
        AudioFormat[] fallbackFormats = {
            // 44.1kHz PCM 16-bit mono (CD quality)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 1, 2, 44100.0f, false),
            // 22kHz PCM 16-bit mono (lower quality)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050.0f, 16, 1, 2, 22050.0f, false),
            // 16kHz PCM 16-bit mono (phone quality)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000.0f, 16, 1, 2, 16000.0f, false),
            // 8kHz PCM 16-bit mono (minimum quality)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0f, 16, 1, 2, 8000.0f, false)
        };
        
        for (AudioFormat format : fallbackFormats) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                logger.debug("Found compatible format: {}", format);
                return format;
            }
        }
        
        logger.warn("No compatible audio format found");
        return null;
    }
    
    /**
     * Start the audio capture thread.
     */
    private void startCaptureThread() {
        captureThread = new Thread(() -> {
            byte[] buffer = new byte[chunkSize];
            
            logger.debug("Audio capture thread started");
            
            try {
                while (isRecording.get() && microphone.isOpen() && !Thread.currentThread().isInterrupted()) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        // Create a copy of the data to avoid buffer reuse issues
                        byte[] audioData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                        
                        // Add to queue (non-blocking to avoid deadlock)
                        if (!audioQueue.offer(audioData)) {
                            logger.warn("Audio queue full, dropping audio data");
                            droppedChunks++;
                        } else {
                            totalBytesRead += bytesRead;
                        }
                        
                        logger.trace("Captured {} bytes of audio data", bytesRead);
                    }
                }
            } catch (Exception e) {
                // Check if the exception is due to thread interruption during shutdown
                if (Thread.currentThread().isInterrupted()) {
                    logger.debug("Audio capture thread interrupted during shutdown");
                } else {
                    logger.error("Audio capture thread error", e);
                }
            } finally {
                logger.debug("Audio capture thread stopped");
            }
        }, "DeviceReader-Capture");
        
        captureThread.setDaemon(true);
        captureThread.start();
    }
    
    /**
     * Read audio data from the microphone.
     * This method blocks until audio data is available or timeout is reached.
     * 
     * @return Audio data chunk, or null if timeout reached or end of stream
     * @throws PalabraException If reading fails
     */
    @Override
    public byte[] read() throws PalabraException {
        if (isClosed.get()) {
            return null;
        }
        
        if (!isRecording.get()) {
            // Auto-initialize if not done yet
            initialize();
        }
        
        try {
            byte[] audioData = audioQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            
            if (audioData != null) {
                logger.trace("Read {} bytes from audio queue", audioData.length);
            } else {
                logger.trace("No audio data available within timeout");
            }
            
            return audioData;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Read operation interrupted");
            return null;
        }
    }
    
    /**
     * Check if the device reader is ready to read data.
     * 
     * @return true if recording and ready to read, false otherwise
     */
    @Override
    public boolean isReady() {
        return isRecording.get() && !isClosed.get() && microphone != null && microphone.isOpen();
    }
    
    /**
     * Stop recording and close the microphone.
     * 
     * @throws PalabraException If closing fails
     */
    @Override
    public void close() throws PalabraException {
        if (isClosed.compareAndSet(false, true)) {
            logger.debug("Closing DeviceReader");
            
            // Stop recording
            isRecording.set(false);
            
            // Stop capture thread
            if (captureThread != null && captureThread.isAlive()) {
                captureThread.interrupt();
                try {
                    captureThread.join(1000); // Wait up to 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Close microphone
            if (microphone != null && microphone.isOpen()) {
                microphone.stop();
                microphone.close();
                logger.info("Microphone closed");
            }
            
            // Clear audio queue
            audioQueue.clear();
            
            logger.debug("DeviceReader closed successfully");
        }
    }
    
    /**
     * Get the audio format being used.
     * 
     * @return Audio format
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }
    
    /**
     * Get the actual audio format being used by the microphone.
     * This may differ from the requested format if format negotiation occurred.
     * 
     * @return Actual AudioFormat being used, or null if not initialized
     */
    public AudioFormat getActualAudioFormat() {
        if (microphone != null && microphone.isOpen()) {
            return microphone.getFormat();
        }
        return null;
    }
    
    /**
     * Get the chunk size being used.
     * 
     * @return Chunk size in bytes
     */
    public int getChunkSize() {
        return chunkSize;
    }
    
    /**
     * Get the current number of audio chunks in the queue.
     * 
     * @return Number of chunks in queue
     */
    public int getQueueSize() {
        return audioQueue.size();
    }
    
    /**
     * Check if currently recording.
     * 
     * @return true if recording, false otherwise
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Get streaming statistics.
     * 
     * @return StreamingStats object with current statistics
     */
    public StreamingStats getStreamingStats() {
        return new StreamingStats(totalBytesRead, droppedChunks, audioQueue.size(), maxQueueCapacity);
    }
    
    /**
     * Reset streaming statistics.
     */
    public void resetStats() {
        totalBytesRead = 0;
        droppedChunks = 0;
        logger.debug("Streaming statistics reset");
    }
    
    /**
     * Check if the audio queue is approaching capacity.
     * 
     * @return true if queue is more than 80% full
     */
    public boolean isQueueNearFull() {
        return audioQueue.size() > (maxQueueCapacity * 0.8);
    }
    
    /**
     * Get the estimated latency based on current queue size.
     * 
     * @return Estimated latency in milliseconds
     */
    public long getEstimatedLatencyMs() {
        if (audioFormat == null) return 0;
        
        int queueSize = audioQueue.size();
        float samplesPerMs = audioFormat.getSampleRate() / 1000.0f;
        int bytesPerSample = audioFormat.getSampleSizeInBits() / 8 * audioFormat.getChannels();
        
        return Math.round((queueSize * chunkSize) / (samplesPerMs * bytesPerSample));
    }
    
    /**
     * Simple streaming statistics class.
     */
    public static class StreamingStats {
        public final long totalBytesRead;
        public final long droppedChunks;
        public final int queueSize;
        public final int maxQueueCapacity;
        public final double queueUtilization;
        
        StreamingStats(long totalBytesRead, long droppedChunks, int queueSize, int maxQueueCapacity) {
            this.totalBytesRead = totalBytesRead;
            this.droppedChunks = droppedChunks;
            this.queueSize = queueSize;
            this.maxQueueCapacity = maxQueueCapacity;
            this.queueUtilization = maxQueueCapacity > 0 ? (double) queueSize / maxQueueCapacity : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("StreamingStats{totalBytes=%d, dropped=%d, queue=%d/%d (%.1f%%)}", 
                               totalBytesRead, droppedChunks, queueSize, maxQueueCapacity, queueUtilization * 100);
        }
    }
}
