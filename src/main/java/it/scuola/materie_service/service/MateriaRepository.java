package it.scuola.materie_service.service;

import it.scuola.materie_service.model.Materia;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository per la tabella "materie".
 * Posizionato nel package service/ come nell'esempio del professore.
 *
 * @Repository  → annotazione esplicita come nell'esempio del professore
 * CrudRepository → come nelle slide e nell'esempio del professore
 */
@Repository
public interface MateriaRepository extends CrudRepository<Materia, UUID> {

    boolean existsByCodice(String codice);

    boolean existsByNome(String nome);

    // Restituisce solo le materie attive (active = true)
    java.util.List<Materia> findAllByActiveTrue();
}
