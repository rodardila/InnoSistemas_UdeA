package co.udea.innosistemas.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamCreateRequestDTO {

    @NotBlank
    @Size(min = 3, max = 20, message = "Team name must be between 3 and 20 characters")
    private String name;

    private Integer statusId; //Opcional si hay uno por defecto

    @Size(max = 2, message = "Puedes seleccionar hasta 2 miembros adicionales")
    private List<Long> userIds;
}