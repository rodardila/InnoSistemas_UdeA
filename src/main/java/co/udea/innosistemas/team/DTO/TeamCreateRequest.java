package co.udea.innosistemas.team.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamCreateRequest {

    @NotBlank
    private String name;

    private Integer statusId; // opcional si hay uno por defecto

    @Size(max = 2, message = "Puedes seleccionar hasta 2 miembros adicionales")
    private List<Long> userIds;
}