package com.vetportal.exception;

/**
 * Custom exception class used to indicate failures during database access or operations.
 * Wraps lower-level SQL exceptions with business logic explanations of failures.
 */
public class DataAccessException extends RuntimeException {

    /**
     * Constructs a new DataAccessException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of the exception
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DataAccessException with the specified detail message.
     *
     * @param message the detail message
     */
    public DataAccessException(String message) {
        super(message);
    }
}
