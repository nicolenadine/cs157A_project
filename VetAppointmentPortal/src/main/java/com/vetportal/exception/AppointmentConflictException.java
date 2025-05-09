package com.vetportal.exception;


// Custom exception class for notifying caller of more specific reasons behind failures.
// This class is used when one of the appointment table's constraints are violated
// 1) UNIQUE (provider, date, time)   2) Only Veterinarians can provide non-vaccination appointments
public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }

    public AppointmentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
