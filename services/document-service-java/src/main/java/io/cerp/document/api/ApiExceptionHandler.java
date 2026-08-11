package io.cerp.document.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Catches malformed tenant/project/subProject ids and unparseable
    // capturedAt timestamps - both are client-supplied strings that reach a
    // UUID.fromString()/Instant.parse() call in DocumentService.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("status", 400, "message", e.getMessage()));
    }
}
