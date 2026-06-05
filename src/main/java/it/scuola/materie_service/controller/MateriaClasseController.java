package it.scuola.materie_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.scuola.materie_service.model.MateriaClasseDTO;
import it.scuola.materie_service.model.MateriaClasseResponseDTO;
import it.scuola.materie_service.service.MateriaClasseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST per le assegnazioni materie-classi.
 */
@Tag(name = "Materie-Classe", description = "Assegnazione delle materie alle classi scolastiche")
@RestController
@RequestMapping("/api/v1/materie-classe")
public class MateriaClasseController {

    @Autowired
    private MateriaClasseService materiaClasseService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /materie-classe?idClasse=... → materie di una classe (tutti i ruoli)
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Restituisce le materie assegnate a una classe")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista restituita con successo"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    @GetMapping
    public ResponseEntity<List<MateriaClasseResponseDTO>> trovaPerClasse(
            @RequestParam UUID idClasse) {
        // @RequestParam → legge il parametro dalla query string (?idClasse=...)
        return ResponseEntity.ok(materiaClasseService.trovaPerClasse(idClasse));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /materie-classe → assegna materia a classe (solo SEGRETERIA)
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Assegna una materia a una classe (solo SEGRETERIA)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Assegnazione creata con successo"),
        @ApiResponse(responseCode = "400", description = "Dati di input non validi"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
        @ApiResponse(responseCode = "403", description = "Ruolo non autorizzato (richiesto: SEGRETERIA)"),
        @ApiResponse(responseCode = "404", description = "Materia non trovata"),
        @ApiResponse(responseCode = "409", description = "Materia già assegnata a questa classe")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('SEGRETERIA', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MateriaClasseResponseDTO> assegnaMateria(
            @RequestBody @Valid MateriaClasseDTO dto) {

        MateriaClasseResponseDTO nuova = materiaClasseService.assegnaMateria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuova);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /materie-classe/{id} → rimuove assegnazione materia-classe (solo SEGRETERIA)
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Rimuove l'assegnazione di una materia da una classe (solo SEGRETERIA)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assegnazione rimossa con successo"),
        @ApiResponse(responseCode = "404", description = "Assegnazione non trovata"),
        @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
        @ApiResponse(responseCode = "403", description = "Ruolo non autorizzato (richiesto: SEGRETERIA)")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SEGRETERIA', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> rimuoviAssegnazione(@PathVariable UUID id) {
        materiaClasseService.rimuoviAssegnazione(id);
        return ResponseEntity.noContent().build();   // 204 No Content
    }
}
