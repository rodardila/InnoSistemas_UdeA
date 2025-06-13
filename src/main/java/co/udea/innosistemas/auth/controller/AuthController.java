package co.udea.innosistemas.auth.controller;

import co.udea.innosistemas.auth.dto.*;
import co.udea.innosistemas.auth.service.AuthService;
import co.udea.innosistemas.common.exception.AuthenticationException;
import co.udea.innosistemas.common.utils.TokenValidator;
import co.udea.innosistemas.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {

    private final AuthService authService;
    private final TokenValidator tokenValidator;

    @Operation(
            summary = "User login",
            description = "Authenticates a user with email and password, returns JWT tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "User account disabled"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        log.debug("Login request received for email: {}", request.getEmail());

        AuthResponseDTO response = authService.login(request);

        log.info("Login successful for user: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get user profile",
            description = "Retrieves the profile information of the authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserProfileResponseDTO> getProfile(Authentication authentication) {
        log.debug("Profile request received");

        User user = validateAndExtractUser(authentication);
        UserProfileResponseDTO profile = authService.getProfile(user);

        log.debug("Profile retrieved successfully for user: {}", user.getEmail());
        return ResponseEntity.ok(profile);
    }

    @Operation(
            summary = "User logout",
            description = "Logs out the user by revoking the access token and optionally the refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "500", description = "Logout operation failed")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<LogoutResponseDTO> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody(required = false) TokenRevokeRequestDTO revokeRequest) {

        log.debug("Logout request received");

        String accessToken = tokenValidator.validateAndExtractToken(authHeader);
        LogoutResponseDTO response = authService.logout(accessToken, revokeRequest);

        log.info("Logout successful");
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @RequestBody @Valid TokenRefreshRequestDTO refreshRequest) {

        log.debug("Token refresh request received");

        AuthResponseDTO response = authService.refreshToken(refreshRequest.getRefreshToken());

        log.info("Token refresh successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Valida la autenticación y extrae el usuario
     */
    private User validateAndExtractUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("Authentication object is null");
            throw new AuthenticationException("Usuario no autenticado");
        }

        if (!(authentication.getPrincipal() instanceof User)) {
            log.warn("Authentication principal is not a User instance: {}",
                    authentication.getPrincipal().getClass().getSimpleName());
            throw new AuthenticationException("Principal de autenticación inválido");
        }

        User user = (User) authentication.getPrincipal();
        log.debug("User validated from authentication: {}", user.getEmail());
        return user;
    }
}