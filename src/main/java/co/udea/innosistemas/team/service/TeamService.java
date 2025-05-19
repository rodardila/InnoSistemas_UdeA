package co.udea.innosistemas.team.service;

import co.udea.innosistemas.team.DTO.TeamCreateRequest;
import co.udea.innosistemas.team.model.Team;
import co.udea.innosistemas.team.model.TeamStatus;
import co.udea.innosistemas.team.repository.TeamRepository;
import co.udea.innosistemas.team.repository.TeamStatusRepository;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamStatusRepository teamStatusRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createTeam(TeamCreateRequest request, User creator) {

        Set<Long> memberIds = new HashSet<>(request.getUserIds());
        memberIds.add(Long.valueOf(creator.getId()));  // Incluir automáticamente al creador

        if (memberIds.size() > 3) {
            throw new IllegalArgumentException("El equipo no puede tener más de 3 integrantes");
        }

        List<User> users = userRepository.findAllById(memberIds);

        if (users.size() != memberIds.size()) {
            throw new IllegalArgumentException("Uno o más usuarios no existen");
        }

        boolean algunoYaEnEquipo = users.stream().anyMatch(user -> user.getTeam() != null);
        if (algunoYaEnEquipo) {
            throw new IllegalArgumentException("Un usuario ya pertenece a un equipo");
        }

        TeamStatus status = teamStatusRepository.findById(1)
                .orElseThrow(() -> new IllegalArgumentException("Estado 'En formación' no encontrado"));

        Team team = Team.builder()
                .name(request.getName())
                .status(status)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        teamRepository.save(team);

        users.forEach(user -> user.setTeam(team));
        userRepository.saveAll(users);
    }
}