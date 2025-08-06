package ai.palabra.adapter;

import ai.palabra.Writer;
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
 * Device-based audio output adapter for live speaker access.
 * Uses Java Sound API to play audio through the system speakers.
 */
public class DeviceWriter extends Writer {
    private static final Logger logger = LoggerFactory.getLogger(DeviceWriter.class);
    
    // Default configuration matching AudioUtils PCM format
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_QUEUE_CAPACITY = 100;
    
    private final AudioFormat audioFormat;
    private final int bufferSize;
    private final BlockingQueue<byte[]> audioQueue;
    private final AtomicBoolean isPlaying;
    private final AtomicBoolean isClosed;
    private final int maxQueueCapacity;
    
    private SourceDataLine speakers;
    private Thread playbackThread;
    private volatile long totalBytesWritten = 0;
    private volatile long droppedChunks = 0;
    
    /**
     * Create a new DeviceWriter with default settings.
     * Uses PCM 16-bit mono 48kHz format matching AudioUtils.
     */
    public DeviceWriter() {
        this(DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Create a new DeviceWriter with custom buffer size.
     * 
     * @param bufferSize Size of the audio buffer (in bytes)
     */
    public DeviceWriter(int bufferSize) {
        this.bufferSize = bufferSize;
        this.audioFormat = AudioUtils.getPcmFormat();
        this.audioQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.isPlaying = new AtomicBoolean(false);
        this.isClosed = new AtomicBoolean(false);
        this.maxQueueCapacity = DEFAULT_QUEUE_CAPACITY;
        
        logger.debug("Created DeviceWriter with bufferSize={}", bufferSize);
    }
    
    /**
     * Create a new DeviceWriter with custom audio format.
     * 
     * @param bufferSize Size of the audio buffer (in bytes)
     * @param audioFormat Custom audio format to use
     */
    public DeviceWriter(int bufferSize, AudioFormat audioFormat) {
        this.bufferSize = bufferSize;
        this.audioFormat = audioFormat;
        this.audioQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.isPlaying = new AtomicBoolean(false);
        this.isClosed = new AtomicBoolean(false);
        this.maxQueueCapacity = DEFAULT_QUEUE_CAPACITY;
        
        logger.debug("Created DeviceWriter with custom format: {}", audioFormat);
    }
    
    /**
     * Initialize and start audio playback to the speakers.
     * Must be called before writing audio data.
     * 
     * @throws PalabraException If speaker initialization fails
     */
    public void initialize() throws PalabraException {
        if (isClosed.get()) {
            throw new PalabraException("Cannot initialize closed DeviceWriter");
        }
        
        if (isPlaying.get()) {
            logger.debug("DeviceWriter already initialized and playing");
            return;
        }
        
        try {
            // Try to find and open speakers with preferred format
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            
            if (!AudioSystem.isLineSupported(info)) {
                // Try to find a compatible format
                AudioFormat compatibleFormat = findCompatibleFormat();
                if (compatibleFormat != null) {
                    logger.info("Using compatible format instead of preferred: {}", compatibleFormat);
                    info = new DataLine.Info(SourceDataLine.class, compatibleFormat);
                } else {
                    throw new PalabraException("No compatible speaker format found");
                }
            }
            
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(info.getFormats()[0], bufferSize);
            speakers.start();
            
            logger.info("Speakers opened successfully with format: {}", speakers.getFormat());
            
            // Start audio playback thread
            startPlaybackThread();
            isPlaying.set(true);
            
        } catch (LineUnavailableException e) {
            logger.error("Failed to open speakers", e);
            throw new PalabraException("Failed to open speakers: " + e.getMessage(), e);
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
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                logger.debug("Found compatible format: {}", format);
                return format;
            }
        }
        
        logger.warn("No compatible audio format found");
        return null;
    }
    
    /**
     * Start the audio playback thread.
     */
    private void startPlaybackThread() {
        playbackThread = new Thread(() -> {
            logger.debug("Audio playback thread started");
            
            try {
                while (isPlaying.get() && speakers.isOpen() && !Thread.currentThread().isInterrupted()) {
                    byte[] audioData = audioQueue.poll(100, TimeUnit.MILLISECONDS);
                    
                    if (audioData != null) {
                        int bytesWritten = speakers.write(audioData, 0, audioData.length);
                        totalBytesWritten += bytesWritten;
                        logger.trace("Played {} bytes of audio data", bytesWritten);
                    }
                }
            } catch (InterruptedException e) {
                // This is expected when shutting down - don't log as error
                logger.debug("Audio playback thread interrupted during shutdown");
                Thread.currentThread().interrupt(); // Restore interrupt status
            } catch (Exception e) {
                logger.error("Audio playback thread error", e);
            } finally {
                logger.debug("Audio playback thread stopped");
            }
        }, "DeviceWriter-Playback");
        
        playbackThread.setDaemon(true);
        playbackThread.start();
    }
    
    /**
     * Write audio data to the speakers.
     * This method is non-blocking and queues the audio data for playback.
     * 
     * @param data Audio data to write
     * @throws PalabraException If writing fails
     */
    @Override
    public void write(byte[] data) throws PalabraException {
        if (data == null || data.length == 0) {
            logger.trace("Ignoring null or empty audio data");
            return;
        }
        
        if (isClosed.get()) {
            throw new PalabraException("Cannot write to closed DeviceWriter");
        }
        
        if (!isPlaying.get()) {
            // Auto-initialize if not done yet
            initialize();
        }
        
        boolean debugMode = System.getProperty("PALABRA_DEBUG", "false").equals("true");
        
        if (debugMode) {
            System.out.println("🔊 Adding " + data.length + " bytes to audio queue (current queue size: " + audioQueue.size() + ")");
        }
        
        try {
            // Offer data to queue with timeout to avoid blocking
            if (!audioQueue.offer(data, 100, TimeUnit.MILLISECONDS)) {
                logger.warn("Audio queue full, dropping audio data");
                droppedChunks++;
                throw new PalabraException("Audio queue full, cannot write data");
            }
            
            logger.trace("Queued {} bytes of audio data for playback", data.length);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PalabraException("Write operation interrupted", e);
        }
    }
    
    /**
     * Write audio data immediately to the speakers (blocking).
     * This bypasses the internal queue and writes directly to the speakers.
     * 
     * @param data Audio data to write
     * @throws PalabraException If writing fails
     */
    public void writeImmediate(byte[] data) throws PalabraException {
        if (data == null || data.length == 0) {
            logger.trace("Ignoring null or empty audio data");
            return;
        }
        
        if (isClosed.get()) {
            throw new PalabraException("Cannot write to closed DeviceWriter");
        }
        
        if (!isPlaying.get()) {
            initialize();
        }
        
        if (speakers == null || !speakers.isOpen()) {
            throw new PalabraException("Speakers not available for immediate write");
        }
        
        int bytesWritten = speakers.write(data, 0, data.length);
        logger.trace("Immediately wrote {} bytes of audio data", bytesWritten);
    }
    
    /**
     * Flush any queued audio data and wait for it to be played.
     * 
     * @throws PalabraException If flushing fails
     */
    public void flush() throws PalabraException {
        if (!isPlaying.get() || speakers == null) {
            return;
        }
        
        try {
            // Wait for queue to drain
            while (!audioQueue.isEmpty() && isPlaying.get()) {
                Thread.sleep(10);
            }
            
            // Drain the speakers buffer
            speakers.drain();
            
            logger.debug("Flushed audio data");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PalabraException("Flush operation interrupted", e);
        }
    }
    
    /**
     * Check if the device writer is ready to write data.
     * 
     * @return true if playing and ready to write, false otherwise
     */
    @Override
    public boolean isReady() {
        return isPlaying.get() && !isClosed.get() && speakers != null && speakers.isOpen();
    }
    
    /**
     * Stop playback and close the speakers.
     * 
     * @throws PalabraException If closing fails
     */
    @Override
    public void close() throws PalabraException {
        if (isClosed.compareAndSet(false, true)) {
            logger.debug("Closing DeviceWriter");
            
            // Stop accepting new audio data
            isPlaying.set(false);
            
            // Wait for audio queue to drain
            boolean debugMode = System.getProperty("PALABRA_DEBUG", "false").equals("true");
            boolean testMode = System.getProperty("PALABRA_TEST_MODE", "false").equals("true");
            logger.debug("Waiting for audio queue to drain (current size: {})", audioQueue.size());
            
            if (debugMode) {
                System.out.println("🔧 DeviceWriter.close() called, current queue size: " + audioQueue.size());
            }
            
            int waitCount = 0;
            int maxWait = testMode ? 10 : 150; // In test mode, wait max 1 second, otherwise 15 seconds
            
            while (!audioQueue.isEmpty() && waitCount < maxWait) {
                int queueSize = audioQueue.size();
                
                if (debugMode && waitCount % 10 == 0) { // Log every second
                    System.out.println("⏳ Waiting for audio queue to drain, current size: " + queueSize + ", waited: " + (waitCount * 100) + "ms");
                }
                
                try {
                    Thread.sleep(100);
                    waitCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (!audioQueue.isEmpty()) {
                logger.warn("Audio queue still has {} items when closing", audioQueue.size());
                if (debugMode) {
                    System.out.println("⚠️ Audio queue still has " + audioQueue.size() + " items after waiting " + (waitCount * 100) + "ms");
                }
            } else {
                logger.debug("Audio queue drained successfully");
                if (debugMode) {
                    System.out.println("✅ Audio queue successfully drained after " + (waitCount * 100) + "ms");
                }
            }
            
            // Stop playback thread
            if (playbackThread != null && playbackThread.isAlive()) {
                playbackThread.interrupt();
                try {
                    playbackThread.join(3000); // Wait up to 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug("Audio playback thread interrupted during shutdown");
                }
            }
            
            // Close speakers
            if (speakers != null && speakers.isOpen()) {
                speakers.drain(); // Wait for remaining audio to finish
                speakers.stop();
                speakers.close();
                logger.info("Speakers closed");
            }
            
            // Clear audio queue
            audioQueue.clear();
            
            logger.debug("DeviceWriter closed successfully");
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
     * Get the actual audio format being used by the speakers.
     * This may differ from the requested format if format negotiation occurred.
     * 
     * @return Actual AudioFormat being used, or null if not initialized
     */
    public AudioFormat getActualAudioFormat() {
        if (speakers != null && speakers.isOpen()) {
            return speakers.getFormat();
        }
        return null;
    }
    
    /**
     * Get the buffer size being used.
     * 
     * @return Buffer size in bytes
     */
    public int getBufferSize() {
        return bufferSize;
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
     * Check if currently playing.
     * 
     * @return true if playing, false otherwise
     */
    public boolean isPlaying() {
        return isPlaying.get();
    }
    
    /**
     * Get streaming statistics.
     * 
     * @return StreamingStats object with current statistics
     */
    public StreamingStats getStreamingStats() {
        return new StreamingStats(totalBytesWritten, droppedChunks, audioQueue.size(), maxQueueCapacity);
    }
    
    /**
     * Reset streaming statistics.
     */
    public void resetStats() {
        totalBytesWritten = 0;
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
        
        return Math.round((queueSize * bufferSize) / (samplesPerMs * bytesPerSample));
    }
    
    /**
     * Check if the output buffer is underrunning.
     * 
     * @return true if queue is nearly empty (less than 20% full)
     */
    public boolean isUnderrunning() {
        return audioQueue.size() < (maxQueueCapacity * 0.2);
    }
    
    /**
     * Simple streaming statistics class.
     */
    public static class StreamingStats {
        public final long totalBytesWritten;
        public final long droppedChunks;
        public final int queueSize;
        public final int maxQueueCapacity;
        public final double queueUtilization;
        
        StreamingStats(long totalBytesWritten, long droppedChunks, int queueSize, int maxQueueCapacity) {
            this.totalBytesWritten = totalBytesWritten;
            this.droppedChunks = droppedChunks;
            this.queueSize = queueSize;
            this.maxQueueCapacity = maxQueueCapacity;
            this.queueUtilization = maxQueueCapacity > 0 ? (double) queueSize / maxQueueCapacity : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("StreamingStats{totalBytes=%d, dropped=%d, queue=%d/%d (%.1f%%)}", 
                               totalBytesWritten, droppedChunks, queueSize, maxQueueCapacity, queueUtilization * 100);
        }
    }
}
