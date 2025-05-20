package co.udea.innosistemas.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRevokeRequestDTO {
    private String refreshToken;
}
