package it.scuola.materie_service.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO di input per assegnare una materia a una classe.
 * Viene ricevuto nel corpo della richiesta POST /materie-classe.
 */
public record MateriaClasseDTO(
        @NotNull(message = "L'id della classe è obbligatorio")
        UUID idClasse,

        @NotNull(message = "L'id della materia è obbligatorio")
        UUID idMateria,

        // Opzionale: se non specificato si usano le ore standard della materia
        Integer oreSettimanaliPersonalizzate
) {}
