package ai.palabra;

/**
 * Abstract base class for audio input adapters.
 * Mirrors the Python library Reader interface.
 */
public abstract class Reader implements AutoCloseable {
    
    /**
     * Reads audio data from the input source.
     * 
     * @return Audio data as byte array, or null if end of stream
     * @throws PalabraException if reading fails
     */
    public abstract byte[] read() throws PalabraException;
    
    /**
     * Closes the reader and releases any resources.
     * 
     * @throws PalabraException if closing fails
     */
    public abstract void close() throws PalabraException;
    
    /**
     * Checks if the reader is ready to read data.
     * 
     * @return true if ready to read, false otherwise
     */
    public abstract boolean isReady();
}
