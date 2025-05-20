package co.udea.innosistemas.team.service;

import co.udea.innosistemas.team.dto.TeamCreateRequestDTO;
import co.udea.innosistemas.team.dto.TeamResponseDTO;
import co.udea.innosistemas.team.model.Team;
import co.udea.innosistemas.team.model.TeamStatus;
import co.udea.innosistemas.team.repository.TeamRepository;
import co.udea.innosistemas.team.repository.TeamStatusRepository;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private static final int MAX_TEAM_MEMBERS = 3;
    private static final int FORMATION_STATUS_ID = 1;

    private final TeamRepository teamRepository;
    private final TeamStatusRepository teamStatusRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponseDTO createTeam(TeamCreateRequestDTO request, User creator) {
        if (request == null) {
            throw new IllegalArgumentException("Team creation request cannot be null");
        }

        if (creator == null) {
            throw new IllegalArgumentException("Team creator cannot be null");
        }

        // Initialize member IDs set with creator
        Set<Long> memberIds = initializeMemberIds(request, creator);
        validateTeamSize(memberIds);

        // Fetch and validate users
        List<User> users = fetchAndValidateUsers(memberIds);
        validateUsersTeamStatus(users);

        // Get team status and create team
        TeamStatus status = getFormationStatus();
        Team team = createTeamEntity(request, status);
        Team savedTeam = teamRepository.save(team);

        // Update user associations
        updateUserTeamAssociations(users, savedTeam);

        // Build and return response
        return buildTeamResponse(savedTeam, creator);
    }

    private Set<Long> initializeMemberIds(TeamCreateRequestDTO request, User creator) {
        Set<Long> memberIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.getUserIds())) {
            memberIds.addAll(request.getUserIds());
        }
        memberIds.add(Long.valueOf(creator.getId()));
        return memberIds;
    }

    private void validateTeamSize(Set<Long> memberIds) {
        if (memberIds.size() > MAX_TEAM_MEMBERS) {
            throw new IllegalArgumentException("Team cannot have more than " + MAX_TEAM_MEMBERS + " members");
        }
    }

    private List<User> fetchAndValidateUsers(Set<Long> memberIds) {
        List<User> users = userRepository.findAllById(memberIds);
        if (users.size() != memberIds.size()) {
            throw new IllegalArgumentException("One or more users do not exist");
        }
        return users;
    }

    private void validateUsersTeamStatus(List<User> users) {
        List<User> usersInTeam = users.stream()
            .filter(user -> user.getTeam() != null)
            .toList();
            
    if (!usersInTeam.isEmpty()) {
        String userEmails = usersInTeam.stream()
                .map(User::getEmail)
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException(
                "Following users already belong to a team: " + userEmails);
    }
}

    private TeamStatus getFormationStatus() {
        return teamStatusRepository.findById(FORMATION_STATUS_ID)
                .orElseThrow(() -> new IllegalStateException("Formation status not found"));
    }

    private Team createTeamEntity(TeamCreateRequestDTO request, TeamStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return Team.builder()
                .name(request.getName())
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateUserTeamAssociations(List<User> users, Team team) {
        users.forEach(user -> user.setTeam(team));
        userRepository.saveAll(users);
    }

    private TeamResponseDTO buildTeamResponse(Team team, User creator) {
        return TeamResponseDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .creatorEmail(creator.getEmail())
                .createdAt(team.getCreatedAt().toLocalDateTime())
                .build();
    }
}