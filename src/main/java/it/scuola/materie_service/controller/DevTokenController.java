package it.scuola.materie_service.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Controller disponibile SOLO nel profilo "dev".
 *
 * Scopo: generare token JWT di test per poter usare Swagger
 * senza avere gli altri microservizi attivi (in particolare utenti-service
 * che normalmente emette i token).
 *
 * @Profile("sviluppo") → Spring non carica questo bean in produzione.
 * Se provi ad avviare con il profilo "prod", questo endpoint non esiste.
 *
 * ⚠️ ATTENZIONE: questo controller NON deve mai essere deployato in produzione!
 */
@Profile("sviluppo")
@Tag(name = "DEV - Token Generator",
     description = "⚠️ Solo sviluppo. Genera token JWT per testare le API con Swagger.")
@RestController
@RequestMapping("/dev")
public class DevTokenController {

    @Value("${jwt.chiave-segreta}")
    private String secret;

    @Value("${jwt.durata-ms}")
    private long durataToken;

    /**
     * Genera un token JWT con il ruolo specificato.
     * Usa la stessa chiave segreta configurata in application.properties,
     * quindi il JwtAuthFilter lo accetterà come valido.
     *
     * Ruoli disponibili (come da CONTEXT_V04.md):
     *   POST /dev/token?ruolo=STUDENTE
     *   POST /dev/token?ruolo=DOCENTE
     *   POST /dev/token?ruolo=SEGRETERIA
     *   POST /dev/token?ruolo=DIRIGENTE
     *   POST /dev/token?ruolo=ADMIN
     *   POST /dev/token?ruolo=SUPER_ADMIN
     */
    @Operation(
        summary = "Genera un token JWT di test",
        description = "Ruoli disponibili: STUDENTE, DOCENTE, SEGRETERIA, DIRIGENTE, ADMIN, SUPER_ADMIN"
    )
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generaToken(
            @RequestParam(defaultValue = "SEGRETERIA") String ruolo) {

        // Stessa logica di JwtService: chiave decodificata da Base64
        SecretKey chiave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));

        String token = Jwts.builder()
                .subject("utente-test@scuola.it")
                .claim("ruolo", ruolo)   // "ruolo" come da specifica del gruppo
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + durataToken))
                .signWith(chiave)
                .compact();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "ruolo", ruolo,
                "istruzioni", "Copia il token, clicca 'Authorize' in Swagger, incolla: Bearer <token>"
        ));
    }
}
