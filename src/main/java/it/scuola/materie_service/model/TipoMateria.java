package it.scuola.materie_service.model;

/**
 * Enum che rappresenta il tipo di una materia scolastica.
 * Usato come colonna nella tabella "materie" con @Enumerated(EnumType.STRING),
 * cioè il valore salvato nel DB sarà la stringa "TEORICA", "PRATICA" o "LABORATORIO".
 */
public enum TipoMateria {
    TEORICA,
    PRATICA,
    LABORATORIO
}
