package co.udea.innosistemas.user.model;

import co.udea.innosistemas.team.model.Team;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id_user")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_name", nullable = false)
    private String name;

    @Column(name = "user_email", nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "user_identity_document", nullable = false, unique = true)
    private String identityDocument;

    @ManyToOne
    @JoinColumn(name = "id_user_role", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "id_user_course")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "id_team")
    private Team team;
}