package co.udea.innosistemas.auth.controller;

import co.udea.innosistemas.auth.DTO.AuthResponse;
import co.udea.innosistemas.auth.DTO.LoginRequest;
import co.udea.innosistemas.auth.DTO.UserProfileResponse;
import co.udea.innosistemas.auth.service.AuthService;
import co.udea.innosistemas.auth.util.JwtUtils;
import co.udea.innosistemas.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();  //Se obtiene el email del user directamente
        return ResponseEntity.ok(authService.getProfile(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtUtils.revokeToken(token);
        }
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }



}