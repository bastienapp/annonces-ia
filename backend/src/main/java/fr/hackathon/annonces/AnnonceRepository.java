package fr.hackathon.annonces;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AnnonceRepository extends JpaRepository<Annonce, Long> {
    List<Annonce> findAllByOrderByIdDesc();
}
