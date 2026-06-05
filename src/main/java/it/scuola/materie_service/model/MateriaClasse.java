package it.scuola.materie_service.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity che mappa la tabella "materie_classe".
 * Rappresenta l'associazione tra una materia e una classe specifica.
 *
 * NOTA ARCHITETTURALE:
 * - "materia" → @ManyToOne: FK reale nel DB (stessa istanza PostgreSQL, stesso servizio)
 * - "idClasse" → UUID semplice: riferimento esterno a composizione-service (no FK fisica,
 *   DB diverso → l'integrità è garantita a livello applicativo, non dal DB)
 */
@Entity
@Table(
    name = "materie_classe",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_materie_classe_composita",
        columnNames = {"id_materia", "id_classe"}
    )
)
public class MateriaClasse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Riferimento esterno (cross-service) → non è una FK nel DB.
     * Contiene l'UUID della classe in composizione-service.
     */
    @Column(name = "id_classe", nullable = false)
    private UUID idClasse;

    /**
     * @ManyToOne  → molte righe di materie_classe possono puntare alla stessa materia
     * @JoinColumn → il nome della colonna FK nel DB è "id_materia"
     */
    @ManyToOne
    @JoinColumn(name = "id_materia", nullable = false)
    private Materia materia;

    /**
     * Sovrascrittura delle ore settimanali standard per questa classe specifica.
     * Se null, si usano le ore standard della materia.
     */
    @Column(name = "ore_settimanali_personalizzate")
    private Integer oreSettimanaliPersonalizzate;

    // ── Costruttori ────────────────────────────────────────────────────────────

    public MateriaClasse() {}

    // ── Getter e Setter ────────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public UUID getIdClasse() { return idClasse; }
    public void setIdClasse(UUID idClasse) { this.idClasse = idClasse; }

    public Materia getMateria() { return materia; }
    public void setMateria(Materia materia) { this.materia = materia; }

    public Integer getOreSettimanaliPersonalizzate() { return oreSettimanaliPersonalizzate; }
    public void setOreSettimanaliPersonalizzate(Integer ore) { this.oreSettimanaliPersonalizzate = ore; }
}
