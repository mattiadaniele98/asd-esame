package it.scuola.materie_service.model;

import java.util.UUID;

/**
 * DTO di output per la risposta relativa a un'assegnazione materia-classe.
 * Costruttore da Entity (stesso pattern dell'esempio del professore).
 */
public record MateriaClasseResponseDTO(
        UUID id,
        UUID idClasse,
        MateriaResponseDTO materia,
        Integer oreSettimanaliPersonalizzate
) {
    // Costruttore da Entity
    public MateriaClasseResponseDTO(MateriaClasse mc) {
        this(
            mc.getId(),
            mc.getIdClasse(),
            new MateriaResponseDTO(mc.getMateria()),
            mc.getOreSettimanaliPersonalizzate()
        );
    }
}
