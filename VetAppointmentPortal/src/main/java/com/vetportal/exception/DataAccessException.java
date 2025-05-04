package com.vetportal.exception;

/**
 * Custom unchecked exception used to indicate failures during database access or operations.
 * Wraps lower-level SQL or JDBC exceptions to decouple service logic from direct database errors.
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
