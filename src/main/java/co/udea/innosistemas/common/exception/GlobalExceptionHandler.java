package co.udea.innosistemas.common.exception;

import co.udea.innosistemas.common.dto.ApiError;
import co.udea.innosistemas.common.exception.DuplicateUserException;
import co.udea.innosistemas.common.exception.InvalidUserDataException;
import co.udea.innosistemas.common.exception.UserNotFoundException;
import co.udea.innosistemas.common.exception.UserRegistrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Excepciones de seguridad
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurityException(SecurityException ex) {
        log.error("Security violation occurred", ex);
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN,
                "Security violation",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied", ex);
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "You don't have permission to perform this action"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Excepciones de equipos
    @ExceptionHandler(TeamCreationException.class)
    public ResponseEntity<ApiError> handleTeamCreationException(TeamCreationException ex) {
        log.error("Team creation failed", ex);
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Team creation failed",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Excepciones de usuarios
    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ApiError> handleUserRegistrationException(UserRegistrationException ex) {
        log.error("User registration failed", ex);
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST,
                "User registration failed",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiError> handleDuplicateUserException(DuplicateUserException ex) {
        log.warn("Attempt to create duplicate user", ex);
        ApiError error = new ApiError(
                HttpStatus.CONFLICT,
                "User already exists",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found", ex);
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND,
                "User not found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ApiError> handleInvalidUserDataException(InvalidUserDataException ex) {
        log.warn("Invalid user data provided", ex);
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Invalid user data",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Excepciones de autenticación
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed", ex);
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ApiError> handleUserDisabledException(UserDisabledException ex) {
        log.warn("Disabled user attempted login", ex);
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN,
                "User account disabled",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiError> handleTokenExpiredException(TokenExpiredException ex) {
        log.warn("Expired token used", ex);
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Token expired",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ApiError> handleTokenRevokedException(TokenRevokedException ex) {
        log.warn("Revoked token used", ex);
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Token revoked",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiError> handleRefreshTokenException(RefreshTokenException ex) {
        log.warn("Refresh token operation failed", ex);
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Refresh token failed",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(LogoutException.class)
    public ResponseEntity<ApiError> handleLogoutException(LogoutException ex) {
        log.error("Logout operation failed", ex);
        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Logout failed",
                "Unable to complete logout operation"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Excepciones de tokens (mantener para compatibilidad)
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid token", ex);
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Invalid token",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Excepciones de validación
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("Validation failed", ex);
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // Excepciones de argumentos ilegales (para casos que no se manejan con excepciones específicas)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument provided", ex);
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Excepción genérica
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "Please contact support if the problem persists"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}