package co.udea.innosistemas.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}