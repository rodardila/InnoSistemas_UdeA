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
    private String role;
    private String course;
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
