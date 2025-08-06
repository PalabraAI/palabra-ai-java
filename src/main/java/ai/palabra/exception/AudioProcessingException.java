package ai.palabra.exception;

import ai.palabra.PalabraException;

/**
 * Exception thrown when audio processing operations fail.
 * This includes issues with audio format conversion, device initialization,
 * and audio data processing.
 */
public class AudioProcessingException extends PalabraException {
    
    /**
     * Creates a new AudioProcessingException with the specified message.
     * 
     * @param message the detail message
     */
    public AudioProcessingException(String message) {
        super(message);
    }
    
    /**
     * Creates a new AudioProcessingException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new AudioProcessingException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public AudioProcessingException(Throwable cause) {
        super(cause);
    }
}
