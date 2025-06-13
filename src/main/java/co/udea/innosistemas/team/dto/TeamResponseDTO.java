package co.udea.innosistemas.team.dto;

import co.udea.innosistemas.user.model.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TeamResponseDTO {
    private Integer id;
    private String name;
    private String creatorEmail;
    private LocalDateTime createdAt;
    private Integer currentMembers;
    private List<MemberDTO> currentMembersDetails;
    private Integer availableSpots;

}