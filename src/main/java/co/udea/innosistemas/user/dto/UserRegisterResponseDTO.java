package co.udea.innosistemas.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRegisterResponseDTO {

    private String email;
    private String roleId;
    private String roleName;
    private String courseId;
    private String courseName;
    private LocalDateTime registeredAt;

}