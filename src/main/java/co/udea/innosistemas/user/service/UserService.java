package co.udea.innosistemas.user.service;

import co.udea.innosistemas.user.dto.UserRegisterResponseDTO;
import co.udea.innosistemas.user.dto.UserRegistrationRequestDTO;
import co.udea.innosistemas.user.dto.UserResponseDTO;
import co.udea.innosistemas.common.exception.DuplicateUserException;
import co.udea.innosistemas.common.exception.InvalidUserDataException;
import co.udea.innosistemas.common.exception.UserRegistrationException;
import co.udea.innosistemas.user.model.*;
import co.udea.innosistemas.user.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserRegisterResponseDTO registerUser(UserRegistrationRequestDTO request) {
        log.debug("Attempting to register user with email: {}", request.getEmail());

        try {
            // Validar datos de entrada
            validateUserRegistrationRequest(request);

            // Verificar duplicados
            checkForDuplicateUser(request);

            // Validar entidades relacionadas
            Role role = validateAndGetRole(request.getRoleId());
            Course course = validateAndGetCourse(request.getCourseId());

            // Crear y guardar usuario
            User user = createUserEntity(request, role, course);
            User savedUser = userRepository.save(user);

            log.info("User registered successfully with email: {}", savedUser.getEmail());
            return buildUserRegisterResponse(savedUser);

        } catch (DuplicateUserException | InvalidUserDataException ex) {
            // Re-lanzar excepciones específicas
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during user registration for email: {}", request.getEmail(), ex);
            throw new UserRegistrationException("Error interno durante el registro del usuario", ex);
        }
    }

    public Page<UserResponseDTO> listUsers(Pageable pageable) {
        log.debug("Fetching users page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        try {
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
        } catch (Exception ex) {
            log.error("Error fetching users list", ex);
            throw new UserRegistrationException("Error al obtener la lista de usuarios", ex);
        }
    }

    // Métodos de validación privados
    private void validateUserRegistrationRequest(UserRegistrationRequestDTO request) {
        if (request == null) {
            throw new InvalidUserDataException("La solicitud de registro no puede ser nula");
        }

        if (!StringUtils.hasText(request.getName())) {
            throw new InvalidUserDataException("El nombre es obligatorio");
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new InvalidUserDataException("El correo electrónico es obligatorio");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new InvalidUserDataException("La contraseña es obligatoria");
        }

        if (!StringUtils.hasText(request.getIdentityDocument())) {
            throw new InvalidUserDataException("El documento de identidad es obligatorio");
        }

        if (request.getRoleId() == null) {
            throw new InvalidUserDataException("El rol es obligatorio");
        }

        // Validar formato del documento
        if (!request.getIdentityDocument().matches("^[0-9]{7,10}$")) {
            throw new InvalidUserDataException("El documento debe contener entre 7 y 10 dígitos");
        }

        // Validar formato del email
        if (!request.getEmail().matches("^[\\w.+\\-]+@udea\\.edu\\.co$")) {
            throw new InvalidUserDataException("El correo debe ser institucional @udea.edu.co");
        }
    }

    private void checkForDuplicateUser(UserRegistrationRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to register duplicate email: {}", request.getEmail());
            throw new DuplicateUserException("El correo electrónico ya está registrado");
        }

        if (userRepository.existsByIdentityDocument(request.getIdentityDocument())) {
            log.warn("Attempt to register duplicate identity document: {}", request.getIdentityDocument());
            throw new DuplicateUserException("El documento de identidad ya está registrado");
        }
    }

    private Role validateAndGetRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    log.warn("Invalid role ID provided: {}", roleId);
                    return new InvalidUserDataException("El rol especificado no existe");
                });
    }

    private Course validateAndGetCourse(Long courseId) {
        if (courseId == null) {
            return null; // Course is optional
        }

        return courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.warn("Invalid course ID provided: {}", courseId);
                    return new InvalidUserDataException("El curso especificado no existe");
                });
    }

    private User createUserEntity(UserRegistrationRequestDTO request, Role role, Course course) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            return User.builder()
                    .name(request.getName().trim())
                    .email(request.getEmail().trim().toLowerCase())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .identityDocument(request.getIdentityDocument().trim())
                    .role(role)
                    .course(course)
                    .enabled(true)
                    .team(null)
                    .registeredAt(now)
                    .build();
        } catch (Exception ex) {
            log.error("Error creating user entity", ex);
            throw new UserRegistrationException("Error al crear la entidad del usuario", ex);
        }
    }

    private UserRegisterResponseDTO buildUserRegisterResponse(User user) {
        try {
            return UserRegisterResponseDTO.builder()
                    .email(user.getEmail())
                    .roleId(user.getRole().getId().toString())
                    .roleName(user.getRole().getName())
                    .courseId(user.getCourse() != null ? user.getCourse().getId().toString() : null)
                    .courseName(user.getCourse() != null ? user.getCourse().getName() : null)
                    .registeredAt(user.getRegisteredAt().toLocalDateTime())
                    .build();
        } catch (Exception ex) {
            log.error("Error building user registration response", ex);
            throw new UserRegistrationException("Error al construir la respuesta de registro", ex);
        }
    }
}