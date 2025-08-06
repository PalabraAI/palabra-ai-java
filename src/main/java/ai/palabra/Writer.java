package ai.palabra;

/**
 * Abstract base class for audio output adapters.
 * Mirrors the Python library Writer interface.
 */
public abstract class Writer implements AutoCloseable {
    
    /**
     * Writes audio data to the output destination.
     * 
     * @param data Audio data to write
     * @throws PalabraException if writing fails
     */
    public abstract void write(byte[] data) throws PalabraException;
    
    /**
     * Closes the writer and releases any resources.
     * 
     * @throws PalabraException if closing fails
     */
    public abstract void close() throws PalabraException;
    
    /**
     * Checks if the writer is ready to write data.
     * 
     * @return true if ready to write, false otherwise
     */
    public abstract boolean isReady();
}
