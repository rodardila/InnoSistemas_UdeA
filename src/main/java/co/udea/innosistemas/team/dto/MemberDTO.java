package co.udea.innosistemas.team.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class MemberDTO {

        private Integer id;
        private String name;
        private String email;
        private String course;

}
