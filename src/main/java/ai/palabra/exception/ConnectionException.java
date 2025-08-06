package ai.palabra.exception;

import ai.palabra.PalabraException;

/**
 * Exception thrown when WebSocket connection or communication fails.
 * This includes connection timeouts, protocol errors, and message sending failures.
 */
public class ConnectionException extends PalabraException {
    
    /**
     * Creates a new ConnectionException with the specified message.
     * 
     * @param message the detail message
     */
    public ConnectionException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ConnectionException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new ConnectionException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
