package co.udea.innosistemas.user.service;

import co.udea.innosistemas.user.dto.UserRegistrationRequestDTO;
import co.udea.innosistemas.user.dto.UserResponseDTO;
import co.udea.innosistemas.user.model.*;
import co.udea.innosistemas.user.repository.*;
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
    public void registerUser(UserRegistrationRequestDTO request) {
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
                .enabled(true)
                .team(null)
                .build();

        userRepository.save(user);
    }

    public Page<UserResponseDTO> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .enabled(user.isEnabled())
                .course(user.getCourse() != null ? user.getCourse().getName() : null)
                .team(user.getTeam() != null
                        ? new UserResponseDTO.TeamDto(user.getTeam().getId(), user.getTeam().getName())
                        : null)
                .build());
    }
}