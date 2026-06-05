package it.scuola.materie_service.model;

/**
 * DTO di input per l'aggiornamento di una materia esistente (PUT /materie/{id}).
 * Tutti i campi sono opzionali: null = non modificare quel campo.
 *
 * NON contiene: id, codice (non modificabile), creatoIl
 * Contiene active: permette di riattivare una materia disattivata (soft delete)
 */
public record MateriaUpdateDTO(
        String nome,
        String descrizione,
        Integer oreSettimanali,
        TipoMateria tipoMateria,
        Boolean active
) {}
