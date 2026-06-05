package it.scuola.materie_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitari di VersioningFilter.
 * Stesso package del filtro → accesso al metodo protected doFilterInternal.
 * I campi @Value vengono iniettati manualmente con ReflectionTestUtils.
 */
@ExtendWith(MockitoExtension.class)
class VersioningFilterTest {

    @InjectMocks
    private VersioningFilter versioningFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RequestDispatcher requestDispatcher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(versioningFilter, "versionFrom", List.of("/materie", "/materie-classe"));
        ReflectionTestUtils.setField(versioningFilter, "versionTo", List.of("/api/v1/materie", "/api/v1/materie-classe"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: doFilterInternal
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("doFilterInternal - fa forward a /api/v1/materie per GET /materie")
    void doFilterInternal_forwardPerPathMaterie() throws Exception {
        when(request.getRequestURI()).thenReturn("/materie");
        when(request.getRequestDispatcher("/api/v1/materie")).thenReturn(requestDispatcher);

        versioningFilter.doFilterInternal(request, response, filterChain);

        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - fa forward a /api/v1/materie/{id} per GET /materie/{id}")
    void doFilterInternal_forwardPerPathMaterieConId() throws Exception {
        when(request.getRequestURI()).thenReturn("/materie/some-uuid");
        when(request.getRequestDispatcher("/api/v1/materie/some-uuid")).thenReturn(requestDispatcher);

        versioningFilter.doFilterInternal(request, response, filterChain);

        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - fa forward per /materie-classe")
    void doFilterInternal_forwardPerPathMaterieClasse() throws Exception {
        when(request.getRequestURI()).thenReturn("/materie-classe");
        when(request.getRequestDispatcher("/api/v1/materie-classe")).thenReturn(requestDispatcher);

        versioningFilter.doFilterInternal(request, response, filterChain);

        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - passa oltre senza forward per path non gestiti")
    void doFilterInternal_passaOltrePerPathSconosciuto() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        versioningFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request, never()).getRequestDispatcher(anyString());
    }
}
