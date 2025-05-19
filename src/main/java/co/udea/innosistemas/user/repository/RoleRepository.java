package co.udea.innosistemas.user.repository;

import co.udea.innosistemas.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}