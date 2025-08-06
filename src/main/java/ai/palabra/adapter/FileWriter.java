package ai.palabra.adapter;

import ai.palabra.Writer;
import ai.palabra.PalabraException;
import ai.palabra.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File-based audio output adapter.
 * Writes PCM audio data to files in WAV format.
 */
public class FileWriter extends Writer {
    private static final Logger logger = LoggerFactory.getLogger(FileWriter.class);
    
    private final String filePath;
    private final ByteArrayOutputStream audioBuffer;
    private boolean isReady;
    private boolean isClosed;
    
    /**
     * Create a new FileWriter for the specified output file.
     * 
     * @param filePath Path where the audio file will be written
     */
    public FileWriter(String filePath) {
        this.filePath = filePath;
        this.audioBuffer = new ByteArrayOutputStream();
        this.isReady = true; // Ready to accept data immediately
        this.isClosed = false;
    }
    
    /**
     * Get the file path being written to.
     * 
     * @return File path
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Write audio data to the buffer.
     * Data will be written to the actual file when close() is called.
     * 
     * @param data PCM audio data to write
     * @throws PalabraException If writing fails
     */
    @Override
    public void write(byte[] data) throws PalabraException {
        if (isClosed) {
            throw new PalabraException("Cannot write to closed FileWriter");
        }
        
        if (data == null || data.length == 0) {
            logger.trace("Ignoring empty audio data");
            return;
        }
        
        try {
            audioBuffer.write(data);
            logger.trace("Buffered {} bytes of audio data", data.length);
        } catch (IOException e) {
            logger.error("Failed to buffer audio data", e);
            throw new PalabraException("Failed to buffer audio data", e);
        }
    }
    
    /**
     * Close the writer and save all buffered audio data to the file as WAV.
     * 
     * @throws PalabraException If closing or file writing fails
     */
    @Override
    public void close() throws PalabraException {
        if (isClosed) {
            logger.debug("FileWriter already closed for: {}", filePath);
            return;
        }
        
        logger.info("Closing FileWriter and saving audio to: {}", filePath);
        
        try {
            byte[] audioData = audioBuffer.toByteArray();
            
            if (audioData.length == 0) {
                logger.warn("No audio data to write to file: {}", filePath);
                // Still create an empty WAV file
            }
            
            Path outputPath = Paths.get(filePath);
            
            // Ensure parent directory exists
            Path parentDir = outputPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Write PCM data as WAV file
            AudioUtils.writePcmToWav(audioData, outputPath);
            
            logger.info("Successfully wrote {} bytes of audio data to WAV file: {}", audioData.length, filePath);
            
        } catch (IOException e) {
            logger.error("Failed to write audio file: {}", filePath, e);
            throw new PalabraException("Failed to write audio file: " + filePath, e);
        } finally {
            // Clean up resources
            try {
                audioBuffer.close();
            } catch (IOException e) {
                logger.warn("Failed to close audio buffer", e);
            }
            isClosed = true;
            isReady = false;
        }
    }
    
    /**
     * Check if the writer is ready to accept data.
     * 
     * @return true if ready to write, false otherwise
     */
    @Override
    public boolean isReady() {
        return isReady && !isClosed;
    }
    
    /**
     * Get the amount of audio data currently buffered.
     * 
     * @return Number of bytes buffered
     */
    public int getBufferedSize() {
        return audioBuffer.size();
    }
    
    /**
     * Check if the writer has been closed.
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        return isClosed;
    }
    
    /**
     * Flush any buffered data and save to file without closing the writer.
     * The writer remains open for additional data.
     * 
     * @throws PalabraException If flushing fails
     */
    public void flush() throws PalabraException {
        if (isClosed) {
            throw new PalabraException("Cannot flush closed FileWriter");
        }
        
        logger.debug("Flushing FileWriter buffer to: {}", filePath);
        
        try {
            byte[] audioData = audioBuffer.toByteArray();
            
            if (audioData.length > 0) {
                Path outputPath = Paths.get(filePath);
                
                // Ensure parent directory exists
                Path parentDir = outputPath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                
                // Write current data to file
                AudioUtils.writePcmToWav(audioData, outputPath);
                
                logger.debug("Flushed {} bytes to file: {}", audioData.length, filePath);
            }
            
        } catch (IOException e) {
            logger.error("Failed to flush audio data to file: {}", filePath, e);
            throw new PalabraException("Failed to flush audio data to file: " + filePath, e);
        }
    }
}
