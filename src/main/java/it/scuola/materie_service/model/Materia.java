package it.scuola.materie_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity che mappa la tabella "materie" nel database db_materie.
 *
 * @Entity  → dice a JPA che questa classe è gestita dall'ORM
 * @Table   → collega la classe alla tabella "materie" nel DB
 */
@Entity
@Table(name = "materie")
public class Materia {

    /**
     * @Id               → questa è la chiave primaria
     * @GeneratedValue   → il valore viene generato automaticamente (UUID casuale)
     * @Column           → specifica il nome della colonna nel DB
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, unique = true, length = 100)
    private String nome;

    @Column(name = "codice", nullable = false, unique = true, length = 10)
    private String codice;

    @Column(name = "descrizione")
    private String descrizione;

    @Column(name = "ore_settimanali")
    private Integer oreSettimanali;

    /**
     * @Enumerated(EnumType.STRING) → salva il nome dell'enum come stringa nel DB
     * (es: "TEORICA", non il numero 0)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_materia", nullable = false)
    private TipoMateria tipoMateria;

    @Column(name = "creato_il", updatable = false)
    private LocalDateTime creatoIl;

    /**
     * Soft delete: true = materia attiva, false = materia disattivata.
     * Se ha riferimenti in materie_classe viene disattivata (active=false),
     * altrimenti viene eliminata fisicamente dal DB (hard delete).
     */
    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    // ── Costruttori ────────────────────────────────────────────────────────────

    public Materia() {}

    // Costruttore da DTO (come User(UserDTO) nell'esempio del professore)
    public Materia(MateriaDTO dto) {
        this.nome = dto.nome();
        this.codice = dto.codice();
        this.descrizione = dto.descrizione();
        this.oreSettimanali = dto.oreSettimanali();
        this.tipoMateria = dto.tipoMateria();
        this.creatoIl = LocalDateTime.now();
    }

    // ── Getter e Setter ────────────────────────────────────────────────────────
    // (necessari per JPA e per la serializzazione JSON di Spring Boot)

    public UUID getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodice() { return codice; }
    public void setCodice(String codice) { this.codice = codice; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public Integer getOreSettimanali() { return oreSettimanali; }
    public void setOreSettimanali(Integer oreSettimanali) { this.oreSettimanali = oreSettimanali; }

    public TipoMateria getTipoMateria() { return tipoMateria; }
    public void setTipoMateria(TipoMateria tipoMateria) { this.tipoMateria = tipoMateria; }

    public LocalDateTime getCreatoIl() { return creatoIl; }
    public void setCreatoIl(LocalDateTime creatoIl) { this.creatoIl = creatoIl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
