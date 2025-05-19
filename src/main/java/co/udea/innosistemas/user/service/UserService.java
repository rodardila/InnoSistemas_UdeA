package co.udea.innosistemas.user.service;

import co.udea.innosistemas.user.DTO.UserRegistrationRequest;
import co.udea.innosistemas.user.DTO.UserResponse;
import co.udea.innosistemas.user.model.*;
import co.udea.innosistemas.user.repository.*;
import co.udea.innosistemas.team.model.Team;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (userRepository.existsByIdentityDocument(request.getIdentityDocument())) {
            throw new IllegalArgumentException("El documento ya está registrado");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido"));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Curso inválido"));
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .identityDocument(request.getIdentityDocument())
                .role(role)
                .course(course)
                .team(null)
                .build();

        userRepository.save(user);
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .course(user.getCourse() != null ? user.getCourse().getName() : null)
                .team(user.getTeam() != null
                        ? new UserResponse.TeamDto(user.getTeam().getId(), user.getTeam().getName())
                        : null)
                .build());
    }
}