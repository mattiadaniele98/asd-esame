package it.scuola.materie_service.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO di output per la risposta al client.
 * Costruttore da Entity (come UserDTO(User) nell'esempio del professore).
 */
public record MateriaResponseDTO(
        UUID id,
        String nome,
        String codice,
        String descrizione,
        Integer oreSettimanali,
        TipoMateria tipoMateria,
        boolean active,
        LocalDateTime creatoIl
) {
    // Costruttore da Entity — evita il metodo toResponseDTO() nel service
    public MateriaResponseDTO(Materia materia) {
        this(
            materia.getId(),
            materia.getNome(),
            materia.getCodice(),
            materia.getDescrizione(),
            materia.getOreSettimanali(),
            materia.getTipoMateria(),
            materia.isActive(),
            materia.getCreatoIl()
        );
    }
}
