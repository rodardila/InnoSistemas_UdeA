package co.udea.innosistemas.team.repository;

import co.udea.innosistemas.team.model.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamStatusRepository extends JpaRepository<TeamStatus, Integer> {
}