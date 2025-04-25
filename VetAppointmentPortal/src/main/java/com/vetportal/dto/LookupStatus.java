package com.vetportal.dto;

/**
 * Enum representing the possible outcomes of a service-layer operation.
 */
public enum LookupStatus {

    /**
     * Indicates the operation completed successfully and returned a result.
     */
    SUCCESS,

    /**
     * Indicates that the requested item was not found.
     */
    NOT_FOUND,

    /**
     * Indicates that the action would create a conflict or violate a constraint
     */
    CONFLICT,

    /**
     * Indicates that an error occurred during database access or processing.
     */
    DB_ERROR
}
