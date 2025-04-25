package com.vetportal.dto;

/**
 * A generic wrapper for service-layer responses.
 * Includes the returned data (if any), a message (typically for errors),
 * and a status indicating the result of the operation.
 *
 * @param <T> the type of data being returned
 */
public class ServiceResponse<T> {
    private T data;
    private String message;
    private LookupStatus status;

    /**
     * Private constructor for internal use.
     *
     * @param data    the data returned by the service (nullable)
     * @param message a message describing the outcome (nullable)
     * @param status  the status of the service operation
     */
    private ServiceResponse(T data, String message, LookupStatus status) {
        this.data = data;
        this.message = message;
        this.status = status;
    }

    // --------  FACTORY METHODS -------

    /**
     * Creates a successful response with the given data.
     *
     * @param data the successful result of the service call
     * @return a ServiceResponse with status SUCCESS
     */
    public static <T> ServiceResponse<T> success(T data) {
        return new ServiceResponse<>(data, null, LookupStatus.SUCCESS);
    }

    /**
     * Creates a response indicating the requested item was not found.
     *
     * @param message a message describing missing data
     * @return a ServiceResponse with status NOT_FOUND
     */
    public static <T> ServiceResponse<T> notFound(String message) {
        return new ServiceResponse<>(null, message, LookupStatus.NOT_FOUND);
    }


    /**
     * Creates a response indicating the requested item was not found.
     *
     * @param message a message describing missing data
     * @return a ServiceResponse with status CONFLICT
     */
    public static <T> ServiceResponse<T> conflict(String message) {
        return new ServiceResponse<>(null, message, LookupStatus.CONFLICT);
    }


    /**
     * Creates a response indicating a database error occurred.
     *
     * @param message a message describing the error
     * @return a ServiceResponse with status DB_ERROR
     */
    public static <T> ServiceResponse<T> dbError(String message) {
        return new ServiceResponse<>(null, message, LookupStatus.DB_ERROR);
    }

    /**
     * Checks if the response status is SUCCESS.
     *
     * @return true if the status is SUCCESS, false otherwise
     */
    public boolean isSuccess() {
        return status == LookupStatus.SUCCESS;
    }

    // -------- GETTERS AND SETTERS --------

    /**
     * Gets the data returned by the service.
     *
     * @return the data (nullable)
     */
    public T getData() {
        return data;
    }

    /**
     * Sets the response data.
     *
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * Gets the message associated with the response.
     *
     * @return the message (nullable)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message for the response.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the status of the response.
     *
     * @return the response status
     */
    public LookupStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the response.
     *
     * @param status the status to set
     */
    public void setStatus(LookupStatus status) {
        this.status = status;
    }
}
