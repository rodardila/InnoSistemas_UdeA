package co.udea.innosistemas.auth.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponseDTO {

    private Integer id;
    private String name;
    private String email;
    private String roleId;
    private String roleName;
    private String courseId;
    private String courseName;
    private TeamDto team;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TeamDto {
        private Integer id;
        private String name;
    }
}
