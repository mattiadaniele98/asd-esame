package it.scuola.materie_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Gestore globale delle eccezioni.
 *
 * @RestControllerAdvice → intercetta le eccezioni lanciate da qualsiasi controller
 * e le trasforma in risposte HTTP con il codice corretto.
 *
 * Questo approccio evita il meccanismo sendError() di Servlet
 * che in Spring Boot 4.x con Spring Security può causare comportamenti inattesi.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gestisce ResourceNotFoundException → 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    /**
     * Gestisce ResponseStatusException (usata per 409 Conflict ecc.)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", ex.getStatusCode().value(),
                "error", ex.getReason() != null ? ex.getReason() : "Error",
                "message", ex.getMessage()
        ));
    }
}
