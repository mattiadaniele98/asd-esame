package it.scuola.materie_service.config;

import it.scuola.materie_service.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configurazione di Spring Security per il microservizio.
 *
 * @Configuration    → questa classe contiene configurazioni Spring
 * @EnableWebSecurity → abilita la sicurezza web
 * @EnableMethodSecurity → abilita @PreAuthorize sui metodi dei controller
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    /**
     * Bypassa completamente Spring Security per Swagger e path pubblici.
     * Questi path non passano nemmeno per il filtro JWT.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/**", "/dev/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disabilita CSRF: non necessario per API REST stateless
            .csrf(csrf -> csrf.disable())

            // Stateless: nessuna sessione HTTP lato server (ogni richiesta porta il JWT)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Swagger UI non richiede autenticazione (è documentazione pubblica)
            // Tutte le altre richieste richiedono un JWT valido
            .authorizeHttpRequests(auth -> auth
                    // Tutto il resto richiede autenticazione JWT
                    .anyRequest().authenticated()
            )

            // 401 quando manca il token, 403 quando il ruolo non è sufficiente
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\":401,\"error\":\"Non autorizzato\","
                                + "\"message\":\"Token JWT mancante o non valido\"}"
                        );
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\":403,\"error\":\"Accesso negato\","
                                + "\"message\":\"Ruolo non autorizzato per questa operazione\"}"
                        );
                    })
            )

            // Aggiunge il filtro JWT prima del filtro standard di Spring Security
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
