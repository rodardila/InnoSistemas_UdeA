package co.udea.innosistemas.common.model;

import co.udea.innosistemas.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {

    @Id
    @Column(name = "id_log")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(name = "event_name", nullable = false, length = 30)
    private String eventName;

    @Column(name = "event_description", length = 255)
    private String eventDescription;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
    }
}