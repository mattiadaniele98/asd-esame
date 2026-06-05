package it.scuola.materie_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Servizio per la validazione e il parsing dei token JWT.
 *
 * Questo servizio NON crea token (lo fa utenti-service).
 * Riceve il token già creato da utenti-service e:
 * 1. Verifica che la firma sia valida (stesso segreto condiviso)
 * 2. Verifica che il token non sia scaduto
 * 3. Estrae le informazioni (subject = email utente, role = ruolo)
 *
 * Come spiegato nelle slide: "è compito di chi riceve il token controllarne la validità"
 */
@Service
public class JwtService {

    /**
     * @Value inietta il valore di jwt.secret da application.properties.
     * In questo modo la chiave non è hardcoded nel codice.
     */
    @Value("${jwt.chiave-segreta}")
    private String secret;

    /**
     * Costruisce la chiave crittografica a partire dalla stringa in Base64.
     * HS256 = cifratura simmetrica (come spiegato nelle slide).
     */
    private SecretKey getSigningKey() {
        // La chiave viene usata come testo semplice (UTF-8), compatibile con
        // Postman JWT Bearer con "Secret Base64 encoded = OFF"
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Verifica che il token sia valido (firma corretta e non scaduto).
     * Ritorna true se valido, false altrimenti.
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Estrae il "subject" dal payload del token.
     * Di solito è l'email o l'username dell'utente.
     */
    public String extractSubject(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Estrae il campo "role" personalizzato dal payload.
     * utenti-service mette il ruolo (STUDENTE/DOCENTE/SEGRETERIA) nel token.
     */
    public String extractRole(String token) {
        // Il claim si chiama "ruolo" come da specifica del gruppo (Postman_JWT_Guide)
        return getClaims(token).get("ruolo", String.class);
    }

    /**
     * Estrae tutti i "claims" (campi del payload) dal token.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
