package it.scuola.materie_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro che gestisce il versioning trasparente delle API.
 *
 * Problema: i controller sono mappati su /api/v1/materie,
 *           ma il client chiama semplicemente /materie.
 *
 * Soluzione: questo filtro intercetta la richiesta PRIMA che arrivi al controller,
 * sostituisce il prefisso /materie con /api/v1/materie e fa un "forward" interno.
 * Il client non sa nulla di questo: vede sempre /materie.
 *
 * Configurato in application.properties:
 *   forward.version.from=/materie, /materie-classe
 *   forward.version.to=/api/v1/materie, /api/v1/materie-classe
 */
@Component
public class VersioningFilter extends OncePerRequestFilter {

    @Value("${forward.version.from}")
    private List<String> versionFrom;  // es: ["/materie", "/materie-classe"]

    @Value("${forward.version.to}")
    private List<String> versionTo;    // es: ["/api/v1/materie", "/api/v1/materie-classe"]

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        for (int i = 0; i < versionFrom.size(); i++) {
            String from = versionFrom.get(i).trim();

            // Controlla se il path inizia con "from" e il carattere successivo è / o ?
            // (evita che /materie faccia match con /materie-classe)
            if (path.startsWith(from) &&
                    (path.length() == from.length() ||
                     path.charAt(from.length()) == '/' ||
                     path.charAt(from.length()) == '?')) {

                String sottoPath = path.substring(from.length());

                // Forward interno: reindirizza a /api/v1/materie + resto del path
                request.getRequestDispatcher(versionTo.get(i).trim() + sottoPath)
                       .forward(request, response);
                return;
            }
        }

        // Nessun match → passa normalmente
        filterChain.doFilter(request, response);
    }
}
