package co.udea.innosistemas.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponseDTO {
    private String message;
    private String errorCode;
    private long timestamp;
}