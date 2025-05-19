package co.udea.innosistemas.team.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatus {

    @Id
    @Column(name = "id_team_status")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "team_status_name", nullable = false)
    private String name;
}