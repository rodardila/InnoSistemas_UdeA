package co.udea.innosistemas.user.service;

import co.udea.innosistemas.team.dto.MemberDTO;
import co.udea.innosistemas.team.dto.TeamCreateRequestDTO;
import co.udea.innosistemas.team.dto.TeamResponseDTO;
import co.udea.innosistemas.team.model.Team;
import co.udea.innosistemas.team.model.TeamStatus;
import co.udea.innosistemas.user.dto.UserRegisterResponseDTO;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserRegisterResponseDTO registerUser(UserRegistrationRequestDTO request) {
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

        User user = createUserEntity(request, role, course);
        User userSaved = userRepository.save(user);

        return buildUserRegisterResponse(userSaved);
    }

    public Page<UserResponseDTO> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roleId(user.getRole().getId().toString())
                .roleName(user.getRole().getName())
                .courseId(user.getCourse() != null ? String.valueOf(user.getCourse().getId()) : null)
                .courseName(user.getCourse() != null ? user.getCourse().getName() : null)
                .enabled(user.isEnabled())
                .team(user.getTeam() != null
                        ? new UserResponseDTO.TeamDto(user.getTeam().getId(), user.getTeam().getName())
                        : null)
                .build());
    }

    private User createUserEntity(UserRegistrationRequestDTO request, Role role, Course course) {
        OffsetDateTime now = OffsetDateTime.now();
        return   User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .identityDocument(request.getIdentityDocument())
                .role(role)
                .course(course)
                .enabled(true)
                .team(null)
                .registeredAt(now)
                .build();
    }

    private UserRegisterResponseDTO buildUserRegisterResponse(User user) {

            String userEmail = user.getEmail();
            String roleId = user.getRole().getId().toString();
            String userRole = user.getRole().getName();
            String userCourseId = user.getCourse() != null ? String.valueOf(user.getCourse().getId()) : null;
            String userCourseName = user.getCourse() != null ? user.getCourse().getName() : null;
            LocalDateTime userRegisteredAt = user.getRegisteredAt().toLocalDateTime();

        return UserRegisterResponseDTO.builder()
                .email(userEmail)
                .roleId(roleId)
                .roleName(userRole)
                .courseId(userCourseId)
                .courseName(userCourseName)
                .registeredAt(userRegisteredAt)
                .build();
    }
}