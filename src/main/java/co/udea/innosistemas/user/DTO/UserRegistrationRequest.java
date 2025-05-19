package co.udea.innosistemas.user.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[0-9]{7,10}$", message = "Documento inválido")
    private String identityDocument;

    @NotBlank
    @Email(regexp = "^[\\w.+\\-]+@udea\\.edu\\.co$", message = "El correo debe ser institucional @udea.edu.co")
    private String email;

    @NotBlank
    private String password;

    private Long roleId;

    private Long courseId;
}