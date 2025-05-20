package co.udea.innosistemas.common.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

@Data
public class ApiError {
    private HttpStatus status;
    private String message;
    private String details;
    private LocalDateTime timestamp;

    public ApiError(HttpStatus status, String message, String details) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
}