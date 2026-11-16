package fr.hackathon.annonces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/annonces")
class AnnonceController {
    private final AnnonceRepository repository;
    private final N8nClient n8n;

    AnnonceController(AnnonceRepository repository, N8nClient n8n) {
        this.repository = repository;
        this.n8n = n8n;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AnnonceDto> lister() {
        return repository.findAllByOrderByIdDesc().stream().map(Annonce::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnonceDto creer(@Valid @RequestBody NouvelleAnnonce demande) {
        String texte = demande.texte().strip();
        // Pas de transaction ouverte pendant l'appel réseau et pas d'écriture si n8n échoue.
        List<String> competences = n8n.analyser(texte);
        return repository.save(new Annonce(texte, competences)).toDto();
    }

    record NouvelleAnnonce(@NotBlank @Size(max = 5000) String texte) {}
}
