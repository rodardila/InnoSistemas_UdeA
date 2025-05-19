package co.udea.innosistemas.team.repository;

import co.udea.innosistemas.team.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Integer> {
}