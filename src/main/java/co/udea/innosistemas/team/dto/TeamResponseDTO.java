package co.udea.innosistemas.team.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TeamResponseDTO {
    private Integer id;
    private String name;
    private String creatorEmail;
    private LocalDateTime createdAt;
    private Integer currentMembers;
    private Integer availableSpots;
}