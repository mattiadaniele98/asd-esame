package it.scuola.materie_service;

import it.scuola.materie_service.controller.MateriaController;
import it.scuola.materie_service.exception.ResourceNotFoundException;
import it.scuola.materie_service.model.MateriaDTO;
import it.scuola.materie_service.model.MateriaResponseDTO;
import it.scuola.materie_service.model.TipoMateria;
import it.scuola.materie_service.service.MateriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari del MateriaController.
 *
 * Stesso pattern del MateriaServiceTest — puro Mockito, nessun contesto Spring.
 * Si testa che il controller:
 * - chiami il service correttamente
 * - restituisca il ResponseEntity con lo status HTTP giusto
 * - passi i dati corretti al client
 *
 * @ExtendWith(MockitoExtension.class) → abilita Mockito senza avviare Spring
 * @Mock                               → crea un service "finto"
 * @InjectMocks                        → crea il controller con il mock iniettato
 */
@ExtendWith(MockitoExtension.class)
class MateriaControllerTest {

    @Mock
    private MateriaService materiaService;

    @InjectMocks
    private MateriaController materiaController;

    // ── Dati di test riutilizzabili ────────────────────────────────────────────
    private MateriaResponseDTO responseDTO;
    private UUID idTest;

    @BeforeEach
    void setUp() {
        idTest = UUID.fromString("11111111-1111-1111-1111-111111111111");
        responseDTO = new MateriaResponseDTO(
                idTest, "Matematica", "MAT",
                "Matematica e calcolo", 5, TipoMateria.TEORICA,
                true, LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: trovaTutte
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("trovaTutte - dovrebbe restituire 200 con la lista delle materie")
    void trovaTutte_dovrebbeRestituire200ConLista() {
        // Arrange
        when(materiaService.trovaTutte()).thenReturn(List.of(responseDTO));

        // Act
        ResponseEntity<List<MateriaResponseDTO>> risposta = materiaController.trovaTutte();

        // Assert
        assertThat(risposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(risposta.getBody()).hasSize(1);
        assertThat(risposta.getBody().get(0).nome()).isEqualTo("Matematica");

        verify(materiaService, times(1)).trovaTutte();
    }

    @Test
    @DisplayName("trovaTutte - dovrebbe restituire 200 con lista vuota se non ci sono materie")
    void trovaTutte_dovrebbeRestituire200ConListaVuota() {
        // Arrange
        when(materiaService.trovaTutte()).thenReturn(List.of());

        // Act
        ResponseEntity<List<MateriaResponseDTO>> risposta = materiaController.trovaTutte();

        // Assert
        assertThat(risposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(risposta.getBody()).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: trovaPerID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("trovaPerID - dovrebbe restituire 200 con la materia trovata")
    void trovaPerID_dovrebbeRestituire200() {
        // Arrange
        when(materiaService.trovaPerID(idTest)).thenReturn(responseDTO);

        // Act
        ResponseEntity<MateriaResponseDTO> risposta = materiaController.trovaPerID(idTest);

        // Assert
        assertThat(risposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(risposta.getBody()).isNotNull();
        assertThat(risposta.getBody().codice()).isEqualTo("MAT");

        verify(materiaService, times(1)).trovaPerID(idTest);
    }

    @Test
    @DisplayName("trovaPerID - dovrebbe propagare ResourceNotFoundException se non trovata")
    void trovaPerID_dovrebbePropagareEccezioneSeNonTrovata() {
        // Arrange: il service lancia eccezione (che Spring mappa su 404)
        when(materiaService.trovaPerID(idTest))
                .thenThrow(new ResourceNotFoundException("Materia non trovata"));

        // Assert: il controller non gestisce l'eccezione, la rilancia
        // (ci pensa @ResponseStatus sulla classe ResourceNotFoundException)
        assertThatThrownBy(() -> materiaController.trovaPerID(idTest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST: creaMateria
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("creaMateria - dovrebbe restituire 201 con la materia creata")
    void creaMateria_dovrebbeRestituire201() {
        // Arrange
        MateriaDTO dto = new MateriaDTO("Matematica", "MAT", "Calcolo", 5, TipoMateria.TEORICA);
        when(materiaService.creaMateria(any(MateriaDTO.class))).thenReturn(responseDTO);

        // Act
        ResponseEntity<MateriaResponseDTO> risposta = materiaController.creaMateria(dto);

        // Assert
        assertThat(risposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(risposta.getBody()).isNotNull();
        assertThat(risposta.getBody().nome()).isEqualTo("Matematica");

        verify(materiaService, times(1)).creaMateria(dto);
    }
}
