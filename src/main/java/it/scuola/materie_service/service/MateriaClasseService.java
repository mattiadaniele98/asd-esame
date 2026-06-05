package it.scuola.materie_service.service;

import it.scuola.materie_service.exception.ResourceNotFoundException;
import it.scuola.materie_service.model.Materia;
import it.scuola.materie_service.model.MateriaClasse;
import it.scuola.materie_service.model.MateriaClasseDTO;
import it.scuola.materie_service.model.MateriaClasseResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class MateriaClasseService {

    @Autowired
    private MateriaClasseRepository materiaClasseRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    public MateriaClasseResponseDTO assegnaMateria(MateriaClasseDTO dto) {
        Materia materia = this.materiaRepository.findById(dto.idMateria())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Materia non trovata con id: " + dto.idMateria()));

        if (this.materiaClasseRepository.existsByIdClasseAndMateriaId(dto.idClasse(), dto.idMateria())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Questa materia è già assegnata alla classe");
        }

        MateriaClasse mc = new MateriaClasse();
        mc.setIdClasse(dto.idClasse());
        mc.setMateria(materia);
        mc.setOreSettimanaliPersonalizzate(dto.oreSettimanaliPersonalizzate());

        // Usa il costruttore MateriaClasseResponseDTO(MateriaClasse)
        return new MateriaClasseResponseDTO(this.materiaClasseRepository.save(mc));
    }

    public List<MateriaClasseResponseDTO> trovaPerClasse(UUID idClasse) {
        Collection<MateriaClasse> lista = this.materiaClasseRepository.findByIdClasse(idClasse);
        return lista.stream()
                .map(MateriaClasseResponseDTO::new)
                .toList();
    }

    public void rimuoviAssegnazione(UUID id) {
        if (!this.materiaClasseRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Assegnazione non trovata con id: " + id);
        }
        this.materiaClasseRepository.deleteById(id);
    }
}
