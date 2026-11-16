package fr.hackathon.annonces;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "annonces")
class Annonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String texte;

    @ElementCollection
    @CollectionTable(name = "annonce_competences", joinColumns = @JoinColumn(name = "annonce_id"))
    @OrderColumn(name = "ordre")
    @Column(name = "competence", nullable = false, length = 255)
    private List<String> competences = new ArrayList<>();

    @Column(nullable = false)
    private Instant creeLe;

    protected Annonce() {}

    Annonce(String texte, List<String> competences) {
        this.texte = texte;
        this.competences.addAll(competences);
        this.creeLe = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    AnnonceDto toDto() {
        return new AnnonceDto(id, texte, List.copyOf(competences), creeLe);
    }
}

record AnnonceDto(Long id, String texte, List<String> competences, Instant creeLe) {}
