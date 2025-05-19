package co.udea.innosistemas.auth.service;

import co.udea.innosistemas.auth.DTO.AuthResponse;
import co.udea.innosistemas.auth.DTO.LoginRequest;
import co.udea.innosistemas.auth.DTO.UserProfileResponse;
import co.udea.innosistemas.auth.util.JwtUtils;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration}")
    private long expiration;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña inválida");
        }

        String accessToken = jwtUtils.generateToken(user.getEmail());
        String refreshToken = UUID.randomUUID().toString(); // Puedes almacenarlo más adelante

        return new AuthResponse(accessToken, refreshToken, "Bearer", expiration / 1000);
    }

    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .course(user.getCourse() != null ? user.getCourse().getName() : null)
                .team(user.getTeam() != null
                        ? new UserProfileResponse.TeamDto(user.getTeam().getId(), user.getTeam().getName())
                        : null)
                .build();
    }

}