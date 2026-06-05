package it.scuola.materie_service.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO di input per la creazione di una materia.
 * Viene ricevuto nel corpo della richiesta POST /materie.
 *
 * È un record Java (immutabile, solo getter generati automaticamente).
 * Le annotazioni @NotBlank / @NotNull abilitano la validazione automatica
 * di Spring Boot quando il controller usa @Valid sul parametro.
 *
 * NON contiene: id, creatoIl (vengono generati dal server, non dal client)
 */
public record MateriaDTO(
        @NotBlank(message = "Il nome è obbligatorio")
        String nome,

        @NotBlank(message = "Il codice è obbligatorio")
        String codice,

        String descrizione,

        @Min(value = 1, message = "Le ore settimanali devono essere almeno 1")
        @Max(value = 40, message = "Le ore settimanali non possono superare 40")
        Integer oreSettimanali,

        @NotNull(message = "Il tipo materia è obbligatorio")
        TipoMateria tipoMateria
) {}
