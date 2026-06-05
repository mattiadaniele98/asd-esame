package it.scuola.materie_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.scuola.materie_service.model.MateriaDTO;
import it.scuola.materie_service.model.MateriaResponseDTO;
import it.scuola.materie_service.model.MateriaUpdateDTO;
import it.scuola.materie_service.service.MateriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST per il catalogo delle materie.
 *
 * @RestController   → questa classe è un controller REST
 *                    (combina @Controller + @ResponseBody)
 * @RequestMapping   → tutte le API di questa classe sono sotto /api/v1/materie
 *                    (il VersioningFilter fa arrivare qui le chiamate a /materie)
 */
/**
 * @Tag → etichetta visualizzata in Swagger UI per raggruppare le API
 */
@Tag(name = "Materie", description = "Catalogo delle materie scolastiche")
@RestController
@RequestMapping("/api/v1/materie")
public class MateriaController {

    /**
     * @Autowired → Spring inietta automaticamente il service
     * (come mostrato nelle slide del professore)
     */
    @Autowired
    private MateriaService materiaService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /materie → elenco completo (tutti i ruoli autenticati)
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Restituisce il catalogo completo delle materie")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista materie restituita con successo"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    @GetMapping
    public ResponseEntity<List<MateriaResponseDTO>> trovaTutte() {
        // ResponseEntity.ok() → risponde con HTTP 200 + il corpo in JSON
        return ResponseEntity.ok(materiaService.trovaTutte());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /materie/{id} → dettaglio singola materia (tutti i ruoli autenticati)
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Restituisce il dettaglio di una materia tramite ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Materia trovata"),
        @ApiResponse(responseCode = "404", description = "Materia non trovata"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MateriaResponseDTO> trovaPerID(
            @PathVariable UUID id) {
        // @PathVariable → prende il valore {id} dall'URL
        return ResponseEntity.ok(materiaService.trovaPerID(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /materie → crea nuova materia (solo SEGRETERIA)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * @PreAuthorize → controlla il ruolo PRIMA di entrare nel metodo.
     * Se l'utente non ha il ruolo SEGRETERIA, Spring restituisce HTTP 403 Forbidden.
     * (Come spiegato nelle slide: "si può controllare il ruolo e restituire 403")
     *
     * @Valid → attiva la validazione del DTO (i @NotBlank, @NotNull nei record)
     * @RequestBody → prende il corpo JSON della richiesta e lo converte nel DTO
     */
    @Operation(summary = "Crea una nuova materia (solo SEGRETERIA)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Materia creata con successo"),
        @ApiResponse(responseCode = "400", description = "Dati di input non validi"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
        @ApiResponse(responseCode = "403", description = "Ruolo non autorizzato (richiesto: SEGRETERIA)"),
        @ApiResponse(responseCode = "409", description = "Materia già esistente con stesso codice o nome")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('SEGRETERIA', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MateriaResponseDTO> creaMateria(
            @RequestBody @Valid MateriaDTO dto) {

        MateriaResponseDTO nuova = materiaService.creaMateria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuova);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /materie/{id} → aggiorna materia (solo SEGRETERIA)
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Aggiorna una materia esistente (solo SEGRETERIA)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Materia aggiornata con successo"),
        @ApiResponse(responseCode = "404", description = "Materia non trovata"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
        @ApiResponse(responseCode = "403", description = "Ruolo non autorizzato (richiesto: SEGRETERIA)")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SEGRETERIA', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MateriaResponseDTO> aggiornaMateria(
            @PathVariable UUID id,
            @RequestBody MateriaUpdateDTO dto) {

        return ResponseEntity.ok(materiaService.aggiornaMateria(id, dto));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /materie/{id} → elimina materia (solo SEGRETERIA)
    // Hard delete se nessun riferimento, soft delete (active=false) se ha riferimenti
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Elimina una materia (solo SEGRETERIA)",
               description = "Hard delete se non ha riferimenti, soft delete (active=false) se ha assegnazioni")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Materia eliminata o disattivata"),
        @ApiResponse(responseCode = "404", description = "Materia non trovata"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
        @ApiResponse(responseCode = "403", description = "Ruolo non autorizzato (richiesto: SEGRETERIA)")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SEGRETERIA', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> eliminaMateria(@PathVariable UUID id) {
        materiaService.eliminaMateria(id);
        return ResponseEntity.noContent().build();   // 204 No Content
    }
}
