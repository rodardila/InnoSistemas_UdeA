package co.udea.innosistemas.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutResponseDTO {
    private String message;
    private boolean accessTokenRevoked;
    private boolean refreshTokenRevoked;
}