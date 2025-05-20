package co.udea.innosistemas.common.exception;

import co.udea.innosistemas.common.dto.ApiError;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ApiError error = new ApiError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}