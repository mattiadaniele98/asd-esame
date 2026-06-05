package it.scuola.materie_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitari di JwtAuthFilter.
 * Stesso package del filtro → accesso ai metodi protected (shouldNotFilter, doFilterInternal).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: shouldNotFilter
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("shouldNotFilter - restituisce true per /swagger-ui (path pubblico)")
    void shouldNotFilter_ritornaVeroPerSwagger() {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        assertThat(jwtAuthFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter - restituisce false per /api/v1/materie (path protetto)")
    void shouldNotFilter_ritornaFalsoPerApiProtetta() {
        when(request.getRequestURI()).thenReturn("/api/v1/materie");

        assertThat(jwtAuthFilter.shouldNotFilter(request)).isFalse();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: doFilterInternal
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("doFilterInternal - passa oltre se non c'è l'header Authorization")
    void doFilterInternal_passaOltreSeHeaderAssente() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - passa oltre se l'header non inizia con Bearer")
    void doFilterInternal_passaOltreSeHeaderNonBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - non imposta autenticazione se il token non è valido")
    void doFilterInternal_nonAutenticaSeTokenNonValido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token.non.valido");
        when(jwtService.isTokenValid("token.non.valido")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal - imposta autenticazione nel SecurityContext con token valido")
    void doFilterInternal_autenticaConTokenValido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token.valido");
        when(jwtService.isTokenValid("token.valido")).thenReturn(true);
        when(jwtService.extractSubject("token.valido")).thenReturn("utente@scuola.it");
        when(jwtService.extractRole("token.valido")).thenReturn("SEGRETERIA");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
