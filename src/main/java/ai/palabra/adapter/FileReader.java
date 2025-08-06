package ai.palabra.adapter;

import ai.palabra.Reader;
import ai.palabra.PalabraException;
import ai.palabra.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File-based audio input adapter.
 * Supports reading audio files in various formats (WAV, OGG) and converting them to PCM 16-bit mono.
 */
public class FileReader extends Reader {
    private static final Logger logger = LoggerFactory.getLogger(FileReader.class);
    private static final int DEFAULT_CHUNK_SIZE = 4096;
    
    private final String filePath;
    private byte[] pcmData;
    private int position;
    private boolean isReady;
    
    /**
     * Create a new FileReader for the specified audio file.
     * 
     * @param filePath Path to the audio file
     */
    public FileReader(String filePath) {
        this.filePath = filePath;
        this.position = 0;
        this.isReady = false;
    }
    
    /**
     * Get the file path being read.
     * 
     * @return File path
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Initialize the file reader by loading and converting the audio file.
     * This method loads the entire file into memory and converts it to PCM format.
     * Must be called before reading.
     */
    public void initialize() throws PalabraException {
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new PalabraException("Audio file not found: " + filePath);
        }
        
        logger.info("Loading audio file: {}", filePath);
        
        try {
            // Read and convert audio file to PCM 16-bit mono
            pcmData = AudioUtils.readAudioFile(path);
            position = 0;
            isReady = true;
            
            logger.info("Successfully loaded {} bytes of PCM audio from {}", pcmData.length, filePath);
            
        } catch (IOException e) {
            logger.error("Failed to load audio file: {}", filePath, e);
            throw new PalabraException("Failed to load audio file: " + filePath, e);
        }
    }
    
    /**
     * Read audio data from the file.
     * 
     * @return Audio data chunk, or null if end of file reached
     * @throws PalabraException If reading fails
     */
    @Override
    public byte[] read() throws PalabraException {
        return read(DEFAULT_CHUNK_SIZE);
    }
    
    /**
     * Read specified amount of audio data from the file.
     * 
     * @param size Maximum number of bytes to read
     * @return Audio data chunk, or null if end of file reached
     * @throws PalabraException If reading fails
     */
    public byte[] read(int size) throws PalabraException {
        if (!isReady) {
            // Auto-initialize if not done yet
            initialize();
        }
        
        if (position >= pcmData.length) {
            logger.debug("End of file reached at position {}", position);
            return null; // End of file
        }
        
        int remainingBytes = pcmData.length - position;
        int bytesToRead = Math.min(size, remainingBytes);
        
        byte[] chunk = new byte[bytesToRead];
        System.arraycopy(pcmData, position, chunk, 0, bytesToRead);
        position += bytesToRead;
        
        logger.trace("Read {} bytes at position {}/{}", bytesToRead, position, pcmData.length);
        
        return chunk;
    }
    
    /**
     * Close the file reader and release resources.
     * 
     * @throws PalabraException If closing fails
     */
    @Override
    public void close() throws PalabraException {
        logger.debug("Closing FileReader for: {}", filePath);
        pcmData = null;
        position = 0;
        isReady = false;
    }
    
    /**
     * Check if the reader is ready for reading.
     * 
     * @return true if ready, false otherwise
     */
    @Override
    public boolean isReady() {
        return isReady;
    }
    
    /**
     * Get the total size of PCM data in bytes.
     * 
     * @return Total size in bytes, or -1 if not initialized
     */
    public long getTotalSize() {
        return pcmData != null ? pcmData.length : -1;
    }
    
    /**
     * Get the current reading position.
     * 
     * @return Current position in bytes
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * Check if end of file has been reached.
     * 
     * @return true if at end of file
     */
    public boolean isEndOfFile() {
        return pcmData != null && position >= pcmData.length;
    }
    
    /**
     * Reset the reading position to the beginning of the file.
     */
    public void reset() {
        position = 0;
        logger.debug("Reset FileReader position to 0");
    }
}
