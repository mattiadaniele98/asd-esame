package it.scuola.materie_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione di Swagger / OpenAPI.
 *
 * Questa classe segue esattamente l'esempio mostrato nelle slide del professore.
 * Genera automaticamente la documentazione delle API leggendo le annotazioni
 * sui controller (@Operation, @Tag, @ApiResponses, ecc.).
 *
 * Aggiunte rispetto all'esempio base del professore:
 * - SecurityScheme JWT: aggiunge il pulsante "Authorize" in Swagger UI
 *   per poter inserire il token Bearer e testare le API protette.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI configOpenApi(
            @Value("${spring.application.name}") String name,
            @Value("${app.version}") String version,
            @Value("${app.description}") String description) {

        // Nome dello schema di sicurezza (usato come riferimento nei controller)
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // Informazioni generali sull'API (visibili in cima alla pagina Swagger)
                .info(new Info()
                        .title(name)
                        .version(version)
                        .description(description)
                        .license(new License()
                                .name("Apache License, Version 2.0")
                                .identifier("Apache-2.0")
                                .url("https://opensource.org/license/apache-2-0/")))

                // Dichiara lo schema di autenticazione JWT (Bearer token)
                // → fa apparire il pulsante "Authorize" in Swagger UI
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Inserisci il token JWT ottenuto da utenti-service")));
    }
}
