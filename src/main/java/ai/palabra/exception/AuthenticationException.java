package ai.palabra.exception;

import ai.palabra.PalabraException;

/**
 * Exception thrown when API authentication or authorization fails.
 * This includes invalid credentials, expired tokens, and permission issues.
 */
public class AuthenticationException extends PalabraException {
    
    /**
     * Creates a new AuthenticationException with the specified message.
     * 
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new AuthenticationException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new AuthenticationException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public AuthenticationException(Throwable cause) {
        super(cause);
    }
}
