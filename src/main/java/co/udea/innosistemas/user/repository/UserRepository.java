package co.udea.innosistemas.user.repository;

import co.udea.innosistemas.team.model.Team;
import co.udea.innosistemas.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByIdentityDocument(String identityDocument);

    Optional<User> findByEmail(String email);

    /**
     * Encuentra todos los usuarios que pertenecen a un equipo específico
     * @param team el equipo
     * @return lista de usuarios del equipo
     */
    List<User> findByTeam(Team team);

    /**
     * Encuentra todos los usuarios que no tienen equipo asignado
     * @return lista de usuarios sin equipo
     */
    List<User> findByTeamIsNull();
}