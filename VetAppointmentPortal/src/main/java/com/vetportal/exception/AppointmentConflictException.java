package com.vetportal.exception;

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }

    public AppointmentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
