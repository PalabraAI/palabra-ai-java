package ai.palabra.cli.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Centralized error handling and user feedback utility for CLI operations.
 * Provides consistent error reporting, logging, and user messaging across all commands.
 */
public class CLIErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CLIErrorHandler.class);
    
    private final boolean verbose;
    private final boolean quiet;
    
    public CLIErrorHandler(boolean verbose, boolean quiet) {
        this.verbose = verbose;
        this.quiet = quiet;
    }
    
    /**
     * Report an error to the user with appropriate detail level.
     */
    public void reportError(String operation, Throwable error) {
        reportError(operation, error, null);
    }
    
    /**
     * Report an error to the user with a custom user-friendly message.
     */
    public void reportError(String operation, Throwable error, String userMessage) {
        
        // Log the full error for debugging
        logger.error("Error during {}: {}", operation, error.getMessage(), error);
        
        if (quiet) {
            return; // Don't show anything in quiet mode
        }
        
        // Show user-friendly message
        if (userMessage != null) {
            System.err.println("Error: " + userMessage);
        } else {
            System.err.println("Error during " + operation + ": " + getUserFriendlyMessage(error));
        }
        
        // Show detailed information in verbose mode
        if (verbose) {
            System.err.println("\nDetailed error information:");
            System.err.println("Operation: " + operation);
            System.err.println("Exception type: " + error.getClass().getSimpleName());
            System.err.println("Message: " + error.getMessage());
            
            if (error.getCause() != null) {
                System.err.println("Caused by: " + error.getCause().getClass().getSimpleName() + ": " + error.getCause().getMessage());
            }
            
            System.err.println("\nStack trace:");
            error.printStackTrace(System.err);
        } else {
            System.err.println("Use --verbose for detailed error information.");
        }
    }
    
    /**
     * Report a validation error (user input error).
     */
    public void reportValidationError(String message) {
        if (!quiet) {
            System.err.println("Error: " + message);
        }
        logger.warn("Validation error: {}", message);
    }
    
    /**
     * Report a warning message.
     */
    public void reportWarning(String message) {
        if (!quiet) {
            System.err.println("Warning: " + message);
        }
        logger.warn(message);
    }
    
    /**
     * Report an informational message.
     */
    public void reportInfo(String message) {
        if (!quiet) {
            System.out.println(message);
        }
        logger.info(message);
    }
    
    /**
     * Report a success message.
     */
    public void reportSuccess(String message) {
        if (!quiet) {
            System.out.println("✓ " + message);
        }
        logger.info("Success: {}", message);
    }
    
    /**
     * Report progress information (only in verbose mode).
     */
    public void reportProgress(String message) {
        if (verbose && !quiet) {
            System.out.println("→ " + message);
        }
        logger.debug("Progress: {}", message);
    }
    
    /**
     * Get appropriate exit code for different error types.
     */
    public int getExitCode(Throwable error) {
        
        // Check for specific error types by examining the full stack trace
        String errorMessage = error.getMessage().toLowerCase();
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        
        // Check the cause chain for deeper error analysis
        Throwable cause = error;
        while (cause != null) {
            String causeMessage = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            String causeClass = cause.getClass().getSimpleName().toLowerCase();
            
            // Authentication/Authorization errors
            if (causeMessage.contains("unauthorized") || causeMessage.contains("forbidden") || 
                causeMessage.contains("401") || causeMessage.contains("403") || 
                causeMessage.contains("user not found") || causeMessage.contains("invalid credentials") ||
                causeMessage.contains("404") && causeMessage.contains("user")) {
                return 2; // Authentication error
            }
            
            // Network/connection errors
            if (causeMessage.contains("connection") || causeMessage.contains("timeout") || 
                causeMessage.contains("network") || causeClass.contains("socket") ||
                causeClass.contains("connect")) {
                return 3; // Network error
            }
            
            // File/IO errors
            if (causeMessage.contains("file not found") || causeMessage.contains("permission denied") ||
                causeMessage.contains("no such file") || causeMessage.contains("does not exist") ||
                causeClass.contains("io") || causeClass.contains("file")) {
                return 4; // File error
            }
            
            // Validation errors
            if (causeClass.contains("illegal") || causeClass.contains("invalid") ||
                causeMessage.contains("invalid") || causeMessage.contains("malformed")) {
                return 5; // Validation error
            }
            
            // Cancellation
            if (causeClass.contains("cancellation") || causeMessage.contains("cancelled")) {
                return 6; // Cancellation
            }
            
            cause = cause.getCause();
        }
        
        // Check top-level error as fallback
        // Authentication/Authorization errors
        if (errorMessage.contains("unauthorized") || errorMessage.contains("forbidden") || 
            errorMessage.contains("401") || errorMessage.contains("403") || 
            errorMessage.contains("user not found") || errorMessage.contains("invalid credentials")) {
            return 2; // Authentication error
        }
        
        // Network/connection errors
        if (errorMessage.contains("connection") || errorMessage.contains("timeout") || 
            errorMessage.contains("network") || errorClass.contains("socket") ||
            errorClass.contains("connect")) {
            return 3; // Network error
        }
        
        // File/IO errors
        if (errorMessage.contains("file not found") || errorMessage.contains("permission denied") ||
            errorMessage.contains("no such file") || errorMessage.contains("does not exist") ||
            errorClass.contains("io") || errorClass.contains("file")) {
            return 4; // File error
        }
        
        // Validation errors
        if (errorClass.contains("illegal") || errorClass.contains("invalid") ||
            errorMessage.contains("invalid") || errorMessage.contains("malformed")) {
            return 5; // Validation error
        }
        
        // Cancellation
        if (errorClass.contains("cancellation") || errorMessage.contains("cancelled")) {
            return 6; // Cancellation
        }
        
        // Default error
        return 1;
    }
    
    /**
     * Convert technical error message to user-friendly message.
     */
    private String getUserFriendlyMessage(Throwable error) {
        
        String message = error.getMessage();
        String errorClass = error.getClass().getSimpleName();
        
        if (message == null) {
            message = errorClass;
        }
        
        // Check the cause chain for better error messages
        Throwable cause = error;
        while (cause != null) {
            String causeMessage = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            
            if (causeMessage.contains("user not found") || causeMessage.contains("401") || causeMessage.contains("403") ||
                (causeMessage.contains("404") && causeMessage.contains("user"))) {
                return "Invalid credentials. Please check your client ID and secret using 'config set'.";
            }
            
            if (causeMessage.contains("connection") || causeMessage.contains("timeout")) {
                return "Network connection failed. Please check your internet connection.";
            }
            
            cause = cause.getCause();
        }
        
        // Transform common technical errors to user-friendly messages
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("user not found") || lowerMessage.contains("401") || lowerMessage.contains("403")) {
            return "Invalid credentials. Please check your client ID and secret using 'config set'.";
        }
        
        if (lowerMessage.contains("connection") || lowerMessage.contains("timeout")) {
            return "Network connection failed. Please check your internet connection.";
        }
        
        if (lowerMessage.contains("FileNotFoundException") || lowerMessage.contains("file not found") || 
            lowerMessage.contains("does not exist")) {
            return "File not found. Please check the file path.";
        }
        
        if (lowerMessage.contains("permission denied") || lowerMessage.contains("access denied")) {
            return "Permission denied. Please check file permissions.";
        }
        
        if (lowerMessage.contains("unsupported") || lowerMessage.contains("invalid format")) {
            return "Unsupported file format. Supported formats: wav";
        }
        
        if (errorClass.contains("IllegalArgument")) {
            return "Invalid argument provided. Please check your command options.";
        }
        
        // Return original message if no transformation applies
        return message;
    }
    
    /**
     * Create error suggestions based on the error type.
     */
    public void suggestSolutions(Throwable error) {
        
        if (quiet) {
            return;
        }
        
        String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        
        // Check the cause chain for better suggestions
        boolean foundSpecificSuggestion = false;
        Throwable cause = error;
        while (cause != null && !foundSpecificSuggestion) {
            String causeMessage = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            
            if (causeMessage.contains("user not found") || causeMessage.contains("401") || causeMessage.contains("403") ||
                (causeMessage.contains("404") && causeMessage.contains("user"))) {
                System.err.println("\nSuggested solutions:");
                System.err.println("• Configure your API credentials: palabra-cli config set client.id <your-id>");
                System.err.println("• Configure your API secret: palabra-cli config set client.secret <your-secret>");
                System.err.println("• Verify your credentials at https://api.palabra.ai");
                foundSpecificSuggestion = true;
            } else if (causeMessage.contains("connection") || causeMessage.contains("timeout")) {
                System.err.println("\nSuggested solutions:");
                System.err.println("• Check your internet connection");
                System.err.println("• Try increasing the timeout with --timeout option");
                System.err.println("• Verify that https://api.palabra.ai is accessible");
                foundSpecificSuggestion = true;
            }
            
            cause = cause.getCause();
        }
        
        if (foundSpecificSuggestion) {
            return;
        }
        
        System.err.println("\nSuggested solutions:");
        
        if (message.contains("user not found") || message.contains("401") || message.contains("403")) {
            System.err.println("• Configure your API credentials: palabra-cli config set client.id <your-id>");
            System.err.println("• Configure your API secret: palabra-cli config set client.secret <your-secret>");
            System.err.println("• Verify your credentials at https://api.palabra.ai");
        } else if (message.contains("connection") || message.contains("timeout")) {
            System.err.println("• Check your internet connection");
            System.err.println("• Try increasing the timeout with --timeout option");
            System.err.println("• Verify that https://api.palabra.ai is accessible");
        } else if (message.contains("file not found") || message.contains("does not exist")) {
            System.err.println("• Check that the input file exists and the path is correct");
            System.err.println("• Use absolute paths to avoid confusion");
            System.err.println("• Verify you have read permissions for the file");
        } else if (message.contains("unsupported") || message.contains("format")) {
            System.err.println("• Supported audio formats: wav");
            System.err.println("• Convert your file to a supported format");
            System.err.println("• Check that your file is not corrupted");
        } else if (errorClass.contains("device") || message.contains("audio")) {
            System.err.println("• List available devices: palabra-cli device --list-devices");
            System.err.println("• Try selecting a different device with -d option");
            System.err.println("• Check that no other application is using the audio device");
        } else {
            System.err.println("• Use --verbose flag to see detailed error information");
            System.err.println("• Check the documentation or examples");
            System.err.println("• Verify all required parameters are provided");
        }
    }
}
