package it.scuola.materie_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Eccezione lanciata quando una risorsa non viene trovata nel DB.
 *
 * @ResponseStatus → quando questa eccezione viene lanciata, Spring Boot
 * risponde automaticamente con HTTP 404 NOT FOUND.
 * (Come spiegato nelle slide del professore — "Risposte negative")
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String messaggio) {
        super(messaggio);
    }
}
