package com.observastack.orderservice.api;

import com.observastack.orderservice.api.dto.ErrorResponse;
import com.observastack.orderservice.domain.DuplicateIdempotencyKeyException;
import com.observastack.orderservice.domain.EmptyOrderException;
import com.observastack.orderservice.domain.IllegalOrderStateException;
import com.observastack.orderservice.domain.MixedCurrencyException;
import com.observastack.orderservice.domain.OrderNotFoundException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * Translates domain and validation failures into HTTP error responses.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    ResponseEntity<ErrorResponse> handleDuplicateIdempotencyKey(DuplicateIdempotencyKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler({
        EmptyOrderException.class,
        MixedCurrencyException.class,
        IllegalOrderStateException.class,
        IllegalArgumentException.class,
        MissingRequestHeaderException.class
    })
    ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message));
    }

    // StockUnavailableException itself has no handler here on purpose:
    // PlaceOrderService catches it and turns it into a CANCELLED order,
    // which is a normal 201 response, not an error. This handler is for
    // the other, genuinely unexpected case — inventory-service down,
    // timed out, or erroring — which InventoryClient deliberately leaves
    // unstranslated so it can't be confused with "inventory said no."
    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ErrorResponse> handleInventoryUnavailable(RestClientException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE, "inventory service is currently unavailable; please retry"));
    }

    // Defense in depth: PlaceOrderRequest's Bean Validation constraints
    // should catch an oversized SKU or price before this point, and a
    // duplicate idempotency key is translated to
    // DuplicateIdempotencyKeyException before it gets here — but if some
    // other database constraint rejects the write anyway, that's still
    // bad input, not a server bug, so a 500 here would be misleading.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "request violates a data constraint"));
    }
}
