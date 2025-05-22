package co.udea.innosistemas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank
    @Email(message = "Email inválido")
    private String email;

    @NotBlank
    private String password;
}