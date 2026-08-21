package com.observastack.inventoryservice.api.dto;

import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * Response body for any request the API rejects.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message) {

    /**
     * @param status  the HTTP status being returned; must not be null
     * @param message a human-readable explanation; must not be null
     * @return the error body, never null
     */
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
