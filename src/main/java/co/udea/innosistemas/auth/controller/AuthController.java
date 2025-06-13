package co.udea.innosistemas.auth.controller;

import co.udea.innosistemas.auth.dto.*;
import co.udea.innosistemas.common.exception.InvalidTokenException;
import co.udea.innosistemas.auth.service.AuthService;
import co.udea.innosistemas.common.utils.TokenValidator;
import co.udea.innosistemas.user.model.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenValidator tokenValidator;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request) {
        try {
            AuthResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .errorCode("INVALID_CREDENTIALS")
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponseDTO.builder()
                            .message("Error during authentication")
                            .errorCode("AUTH_ERROR")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponseDTO.builder()
                                .message("User not authenticated")
                                .errorCode("NOT_AUTHENTICATED")
                                .timestamp(System.currentTimeMillis())
                                .build());
            }

            User user = (User) authentication.getPrincipal();
            UserProfileResponseDTO profile = authService.getProfile(user);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponseDTO.builder()
                            .message("Error retrieving user profile")
                            .errorCode("PROFILE_ERROR")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody(required = false) TokenRevokeRequestDTO revokeRequest) {
        try {
            String accessToken = tokenValidator.validateAndExtractToken(authHeader);
            LogoutResponseDTO response = authService.logout(accessToken, revokeRequest);
            return ResponseEntity.ok(response);
        } catch (InvalidTokenException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .errorCode("INVALID_TOKEN")
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponseDTO.builder()
                            .message("Error during logout")
                            .errorCode("LOGOUT_ERROR")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }

    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid TokenRefreshRequestDTO refreshRequest) {
        try {
            AuthResponseDTO response = authService.refreshToken(refreshRequest.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (InvalidTokenException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .errorCode("INVALID_TOKEN")
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponseDTO.builder()
                            .message("Error processing refresh token")
                            .errorCode("REFRESH_TOKEN_ERROR")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
}