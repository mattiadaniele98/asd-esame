package it.scuola.materie_service.service;

import it.scuola.materie_service.exception.ResourceNotFoundException;
import it.scuola.materie_service.model.Materia;
import it.scuola.materie_service.model.MateriaDTO;
import it.scuola.materie_service.model.MateriaResponseDTO;
import it.scuola.materie_service.model.MateriaUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class MateriaService {

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private MateriaClasseRepository materiaClasseRepository;

    public MateriaResponseDTO creaMateria(MateriaDTO dto) {
        if (this.materiaRepository.existsByCodice(dto.codice())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Esiste già una materia con codice: " + dto.codice());
        }
        if (this.materiaRepository.existsByNome(dto.nome())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Esiste già una materia con nome: " + dto.nome());
        }

        // Usa il costruttore Materia(MateriaDTO) — come User(UserDTO) nel prof
        Materia salvata = this.materiaRepository.save(new Materia(dto));

        return new MateriaResponseDTO(salvata);
    }

    public List<MateriaResponseDTO> trovaTutte() {
        // Restituisce solo le materie attive (active = true)
        return this.materiaRepository.findAllByActiveTrue()
                .stream()
                .map(MateriaResponseDTO::new)
                .toList();
    }

    public MateriaResponseDTO trovaPerID(UUID id) {
        Materia materia = this.materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Materia non trovata con id: " + id));
        return new MateriaResponseDTO(materia);
    }

    public MateriaResponseDTO aggiornaMateria(UUID id, MateriaUpdateDTO dto) {
        Materia materia = this.materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Materia non trovata con id: " + id));

        // Aggiorna solo i campi non nulli (partial update)
        if (dto.nome() != null) {
            materia.setNome(dto.nome());
        }
        if (dto.descrizione() != null) {
            materia.setDescrizione(dto.descrizione());
        }
        if (dto.oreSettimanali() != null) {
            materia.setOreSettimanali(dto.oreSettimanali());
        }
        if (dto.tipoMateria() != null) {
            materia.setTipoMateria(dto.tipoMateria());
        }
        if (dto.active() != null) {
            materia.setActive(dto.active());
        }

        return new MateriaResponseDTO(this.materiaRepository.save(materia));
    }

    public void eliminaMateria(UUID id) {
        Materia materia = this.materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Materia non trovata con id: " + id));

        if (this.materiaClasseRepository.existsByMateriaId(id)) {
            // Ha riferimenti in materie_classe → soft delete (active = false)
            materia.setActive(false);
            this.materiaRepository.save(materia);
        } else {
            // Nessun riferimento → hard delete (eliminazione fisica)
            this.materiaRepository.deleteById(id);
        }
    }
}
