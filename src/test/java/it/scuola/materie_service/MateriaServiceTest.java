package it.scuola.materie_service;

import it.scuola.materie_service.exception.ResourceNotFoundException;
import it.scuola.materie_service.model.Materia;
import it.scuola.materie_service.model.MateriaDTO;
import it.scuola.materie_service.model.MateriaResponseDTO;
import it.scuola.materie_service.model.TipoMateria;
import it.scuola.materie_service.service.MateriaRepository;
import it.scuola.materie_service.service.MateriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari del MateriaService.
 *
 * Come spiegato nelle slide del professore:
 * - Il testing di unità va fatto a livello di @Service
 * - Si usano i Mock per sostituire le dipendenze reali (repository)
 * - In questo modo il test non tocca il database
 *
 * @ExtendWith(MockitoExtension.class) → abilita Mockito senza avviare Spring
 * @Mock                               → crea un repository "finto" controllabile
 * @InjectMocks                        → crea il service e inietta i mock
 */
@ExtendWith(MockitoExtension.class)
class MateriaServiceTest {

    // ── Mock delle dipendenze ──────────────────────────────────────────────────
    @Mock
    private MateriaRepository materiaRepository;

    // ── Classe sotto test (con i mock iniettati) ───────────────────────────────
    @InjectMocks
    private MateriaService materiaService;

    // ── Dati di test riutilizzabili ────────────────────────────────────────────
    private Materia materiaEsistente;
    private UUID idEsistente;

    /**
     * @BeforeEach → viene eseguito PRIMA di ogni test.
     * Prepara i dati iniziali garantendo isolamento tra test.
     */
    @BeforeEach
    void setUp() {
        idEsistente = UUID.randomUUID();
        materiaEsistente = new Materia();
        materiaEsistente.setNome("Matematica");
        materiaEsistente.setCodice("MAT");
        materiaEsistente.setDescrizione("Matematica e calcolo");
        materiaEsistente.setOreSettimanali(5);
        materiaEsistente.setTipoMateria(TipoMateria.TEORICA);
        materiaEsistente.setCreatoIl(LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: creaMateria
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("creaMateria - dovrebbe creare e restituire la materia")
    void creaMateria_dovrebbeCreareLaMateria() {
        // ── Arrange: prepara input e comportamento del mock ────────────────────
        MateriaDTO dto = new MateriaDTO("Matematica", "MAT", "Calcolo", 5, TipoMateria.TEORICA);

        // Il mock dice: existsByCodice → false (codice non esiste ancora)
        when(materiaRepository.existsByCodice("MAT")).thenReturn(false);
        when(materiaRepository.existsByNome("Matematica")).thenReturn(false);
        // Il mock dice: save → restituisci l'entity salvata
        when(materiaRepository.save(any(Materia.class))).thenReturn(materiaEsistente);

        // ── Act: esegui il metodo da testare ───────────────────────────────────
        MateriaResponseDTO risultato = materiaService.creaMateria(dto);

        // ── Assert: verifica il risultato con AssertJ ──────────────────────────
        assertThat(risultato).isNotNull();
        assertThat(risultato.nome()).isEqualTo("Matematica");
        assertThat(risultato.codice()).isEqualTo("MAT");
        assertThat(risultato.tipoMateria()).isEqualTo(TipoMateria.TEORICA);

        // Verifica che save() sia stato chiamato esattamente 1 volta
        verify(materiaRepository, times(1)).save(any(Materia.class));
    }

    @Test
    @DisplayName("creaMateria - dovrebbe lanciare eccezione se codice già esiste")
    void creaMateria_dovrebbeLanciareEccezioneSeCodiceDuplicato() {
        // Arrange
        MateriaDTO dto = new MateriaDTO("Matematica", "MAT", null, 5, TipoMateria.TEORICA);
        when(materiaRepository.existsByCodice("MAT")).thenReturn(true); // codice già presente

        // Assert: verifica che venga lanciata un'eccezione
        assertThatThrownBy(() -> materiaService.creaMateria(dto))
                .isInstanceOf(RuntimeException.class);

        // Verifica che save() NON venga mai chiamato
        verify(materiaRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: trovaTutte
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("trovaTutte - dovrebbe restituire lista di materie attive")
    void trovaTutte_dovrebbeRestituireLista() {
        // Arrange: il mock restituisce una lista con una materia attiva
        when(materiaRepository.findAllByActiveTrue()).thenReturn(List.of(materiaEsistente));

        // Act
        List<MateriaResponseDTO> risultato = materiaService.trovaTutte();

        // Assert
        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).nome()).isEqualTo("Matematica");
    }

    @Test
    @DisplayName("trovaTutte - dovrebbe restituire lista vuota se non ci sono materie attive")
    void trovaTutte_dovrebbeRestituireListaVuota() {
        // Arrange: il mock restituisce lista vuota
        when(materiaRepository.findAllByActiveTrue()).thenReturn(List.of());

        // Act
        List<MateriaResponseDTO> risultato = materiaService.trovaTutte();

        // Assert
        assertThat(risultato).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: trovaPerID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("trovaPerID - dovrebbe restituire la materia se l'ID esiste")
    void trovaPerID_dovrebbeRestituireLaMateria() {
        // Arrange
        when(materiaRepository.findById(idEsistente))
                .thenReturn(Optional.of(materiaEsistente));

        // Act
        MateriaResponseDTO risultato = materiaService.trovaPerID(idEsistente);

        // Assert
        assertThat(risultato).isNotNull();
        assertThat(risultato.nome()).isEqualTo("Matematica");
    }

    @Test
    @DisplayName("trovaPerID - dovrebbe lanciare ResourceNotFoundException se ID non esiste")
    void trovaPerID_dovrebbeLanciareEccezioneSeIDNonEsiste() {
        // Arrange: il mock restituisce Optional vuoto (materia non trovata)
        UUID idInesistente = UUID.randomUUID();
        when(materiaRepository.findById(idInesistente)).thenReturn(Optional.empty());

        // Assert: verifica che venga lanciata ResourceNotFoundException
        assertThatThrownBy(() -> materiaService.trovaPerID(idInesistente))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(idInesistente.toString());
    }
}
