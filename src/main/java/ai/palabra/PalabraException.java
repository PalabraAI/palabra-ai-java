package ai.palabra;

/**
 * Base exception class for all Palabra AI client library errors.
 * This is the root exception that encompasses all types of failures that can occur
 * when using the Palabra AI Java client library.
 * 
 * <p>Specific subtypes of this exception provide more granular error handling:
 * <ul>
 *   <li>{@code AudioProcessingException} - Audio-related errors</li>
 *   <li>{@code AuthenticationException} - Authentication/authorization errors</li>
 *   <li>{@code ConnectionException} - Network/WebSocket connection errors</li>
 * </ul>
 */
public class PalabraException extends Exception {
    
    /**
     * Constructs a new exception with the specified detail message.
     * 
     * @param message the detail message explaining the error
     */
    public PalabraException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param message the detail message explaining the error
     * @param cause the underlying cause of the error
     */
    public PalabraException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     * 
     * @param cause the underlying cause of the error
     */
    public PalabraException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Constructs a new exception with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     * 
     * @param message the detail message
     * @param cause the cause
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    protected PalabraException(String message, Throwable cause,
                              boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
