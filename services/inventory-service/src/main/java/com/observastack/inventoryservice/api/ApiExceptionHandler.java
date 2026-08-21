package com.observastack.inventoryservice.api;

import com.observastack.inventoryservice.api.dto.ErrorResponse;
import com.observastack.inventoryservice.domain.ConcurrentStockUpdateException;
import com.observastack.inventoryservice.domain.DuplicateReservationException;
import com.observastack.inventoryservice.domain.EmptyReservationException;
import com.observastack.inventoryservice.domain.IllegalReservationStateException;
import com.observastack.inventoryservice.domain.InsufficientStockException;
import com.observastack.inventoryservice.domain.ReservationNotFoundException;
import com.observastack.inventoryservice.domain.StockItemNotFoundException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and validation failures into HTTP error responses.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler({StockItemNotFoundException.class, ReservationNotFoundException.class})
    ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // 409, not 400: the request is well-formed, it just can't be honoured
    // against the resource's current state — there's either not enough
    // stock right now, or another request already claimed this order id,
    // or contention meant the write couldn't land. All three are
    // legitimate to retry, unlike a genuinely malformed request.
    @ExceptionHandler({InsufficientStockException.class, DuplicateReservationException.class, ConcurrentStockUpdateException.class})
    ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler({EmptyReservationException.class, IllegalReservationStateException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message));
    }

    // Defense in depth, same reasoning as order-service's handler: Bean
    // Validation should catch shape problems before this point, but if a
    // database constraint rejects the write anyway, that's bad input, not
    // a server bug.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "request violates a data constraint"));
    }
}
