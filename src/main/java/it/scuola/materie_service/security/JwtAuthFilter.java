package it.scuola.materie_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Filtro JWT: intercetta OGNI richiesta HTTP prima che arrivi al controller.
 *
 * OncePerRequestFilter → garantisce che venga eseguito una sola volta per richiesta.
 *
 * Funzionamento (come descritto nelle slide):
 * 1. Legge l'header "Authorization: Bearer <token>"
 * 2. Valida il token con JwtService
 * 3. Se valido, estrae username e ruolo e li mette nel SecurityContext
 *    (Spring Security userà queste info per controllare @PreAuthorize)
 * 4. Se non c'è il token o non è valido, la richiesta continua
 *    (ma verrà bloccata dalla SecurityConfig se l'endpoint richiede autenticazione)
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/swagger-ui", "/v3/", "/dev/"
    );

    @Autowired
    private JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Legge l'header Authorization
        String authHeader = request.getHeader("Authorization");

        // Se non c'è l'header o non inizia con "Bearer ", passa al filtro successivo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Estrae il token (rimuove il prefisso "Bearer ")
        String token = authHeader.substring(7);

        // Valida il token e autentica l'utente (se non è già autenticato)
        if (jwtService.isTokenValid(token) &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            String username = jwtService.extractSubject(token);
            String role     = jwtService.extractRole(token);

            // Crea l'oggetto di autenticazione con il ruolo estratto dal token
            // "ROLE_" è il prefisso richiesto da Spring Security per i ruoli
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            // Mette l'autenticazione nel SecurityContext (Spring la userà nei controller)
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Passa la richiesta al prossimo filtro (poi arriva al controller)
        filterChain.doFilter(request, response);
    }
}
