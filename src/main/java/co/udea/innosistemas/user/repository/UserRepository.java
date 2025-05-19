package co.udea.innosistemas.user.repository;

import co.udea.innosistemas.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByIdentityDocument(String identityDocument);

    Optional<User> findByEmail(String email);
}