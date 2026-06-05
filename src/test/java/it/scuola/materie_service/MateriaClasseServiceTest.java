package it.scuola.materie_service;

import it.scuola.materie_service.exception.ResourceNotFoundException;
import it.scuola.materie_service.model.Materia;
import it.scuola.materie_service.model.MateriaClasse;
import it.scuola.materie_service.model.MateriaClasseDTO;
import it.scuola.materie_service.model.MateriaClasseResponseDTO;
import it.scuola.materie_service.model.TipoMateria;
import it.scuola.materie_service.service.MateriaClasseRepository;
import it.scuola.materie_service.service.MateriaClasseService;
import it.scuola.materie_service.service.MateriaRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitari del MateriaClasseService.
 * Puro Mockito, nessun contesto Spring, nessun database.
 */
@ExtendWith(MockitoExtension.class)
class MateriaClasseServiceTest {

    @Mock
    private MateriaClasseRepository materiaClasseRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @InjectMocks
    private MateriaClasseService materiaClasseService;

    private Materia materia;
    private MateriaClasse materiaClasse;
    private UUID idMateria;
    private UUID idClasse;
    private UUID idAssegnazione;

    @BeforeEach
    void setUp() {
        idMateria = UUID.randomUUID();
        idClasse = UUID.randomUUID();
        idAssegnazione = UUID.randomUUID();

        materia = new Materia();
        materia.setNome("Matematica");
        materia.setCodice("MAT");
        materia.setTipoMateria(TipoMateria.TEORICA);
        materia.setOreSettimanali(5);
        materia.setCreatoIl(LocalDateTime.now());

        materiaClasse = new MateriaClasse();
        materiaClasse.setIdClasse(idClasse);
        materiaClasse.setMateria(materia);
        materiaClasse.setOreSettimanaliPersonalizzate(4);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: assegnaMateria
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("assegnaMateria - dovrebbe assegnare la materia alla classe")
    void assegnaMateria_dovrebbeAssegnare() {
        MateriaClasseDTO dto = new MateriaClasseDTO(idClasse, idMateria, 4);
        when(materiaRepository.findById(idMateria)).thenReturn(Optional.of(materia));
        when(materiaClasseRepository.existsByIdClasseAndMateriaId(idClasse, idMateria)).thenReturn(false);
        when(materiaClasseRepository.save(any(MateriaClasse.class))).thenReturn(materiaClasse);

        MateriaClasseResponseDTO risultato = materiaClasseService.assegnaMateria(dto);

        assertThat(risultato).isNotNull();
        assertThat(risultato.idClasse()).isEqualTo(idClasse);
        verify(materiaClasseRepository, times(1)).save(any(MateriaClasse.class));
    }

    @Test
    @DisplayName("assegnaMateria - dovrebbe lanciare eccezione se la materia non esiste")
    void assegnaMateria_dovrebbeLanciareEccezioneSeMateriaNonTrovata() {
        MateriaClasseDTO dto = new MateriaClasseDTO(idClasse, idMateria, null);
        when(materiaRepository.findById(idMateria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaClasseService.assegnaMateria(dto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(materiaClasseRepository, never()).save(any());
    }

    @Test
    @DisplayName("assegnaMateria - dovrebbe lanciare eccezione se la materia è già assegnata")
    void assegnaMateria_dovrebbeLanciareEccezioneSeGiaAssegnata() {
        MateriaClasseDTO dto = new MateriaClasseDTO(idClasse, idMateria, null);
        when(materiaRepository.findById(idMateria)).thenReturn(Optional.of(materia));
        when(materiaClasseRepository.existsByIdClasseAndMateriaId(idClasse, idMateria)).thenReturn(true);

        assertThatThrownBy(() -> materiaClasseService.assegnaMateria(dto))
                .isInstanceOf(RuntimeException.class);
        verify(materiaClasseRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: trovaPerClasse
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("trovaPerClasse - dovrebbe restituire le assegnazioni della classe")
    void trovaPerClasse_dovrebbeRestituireLista() {
        when(materiaClasseRepository.findByIdClasse(idClasse)).thenReturn(List.of(materiaClasse));

        List<MateriaClasseResponseDTO> risultato = materiaClasseService.trovaPerClasse(idClasse);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).idClasse()).isEqualTo(idClasse);
    }

    @Test
    @DisplayName("trovaPerClasse - dovrebbe restituire lista vuota se nessuna assegnazione")
    void trovaPerClasse_dovrebbeRestituireListaVuota() {
        when(materiaClasseRepository.findByIdClasse(idClasse)).thenReturn(List.of());

        List<MateriaClasseResponseDTO> risultato = materiaClasseService.trovaPerClasse(idClasse);

        assertThat(risultato).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: rimuoviAssegnazione
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("rimuoviAssegnazione - dovrebbe eliminare l'assegnazione")
    void rimuoviAssegnazione_dovrebbeRimuovere() {
        when(materiaClasseRepository.existsById(idAssegnazione)).thenReturn(true);

        materiaClasseService.rimuoviAssegnazione(idAssegnazione);

        verify(materiaClasseRepository, times(1)).deleteById(idAssegnazione);
    }

    @Test
    @DisplayName("rimuoviAssegnazione - dovrebbe lanciare eccezione se l'assegnazione non esiste")
    void rimuoviAssegnazione_dovrebbeLanciareEccezioneSeNonTrovata() {
        when(materiaClasseRepository.existsById(idAssegnazione)).thenReturn(false);

        assertThatThrownBy(() -> materiaClasseService.rimuoviAssegnazione(idAssegnazione))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(materiaClasseRepository, never()).deleteById(any());
    }
}
