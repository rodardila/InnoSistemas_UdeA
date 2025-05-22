package co.udea.innosistemas.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "revoked_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private OffsetDateTime expiration;

    @Column(name = "revoked_at", nullable = false)
    private OffsetDateTime revokedAt = OffsetDateTime.now();

    @Column(name = "token_type")
    private String tokenType; // "access" o "refresh"
}
