package co.udea.innosistemas.team.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamUpdateRequestDTO {

    @Size(min = 3, max = 20, message = "Team name must be between 3 and 20 characters")
    private String name;

    private Integer statusId;

    @Size(max = 2, message = "Puedes agregar hasta 2 miembros adicionales")
    private List<Long> addUserIds; // IDs de usuarios que se quieren AGREGAR al equipo

    @Size(max = 2, message = "Puedes remover hasta 2 miembros")
    private List<Long> removeUserIds; // IDs de usuarios que se quieren REMOVER del equipo
}