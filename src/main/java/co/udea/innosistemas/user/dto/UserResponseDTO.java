package co.udea.innosistemas.user.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {
    private Integer id;
    private String name;
    private String email;
    private String roleId;
    private String roleName;
    private String courseId;
    private String courseName;
    private boolean enabled;
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