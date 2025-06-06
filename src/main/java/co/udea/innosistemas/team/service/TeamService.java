package co.udea.innosistemas.team.service;

import co.udea.innosistemas.team.dto.TeamCreateRequestDTO;
import co.udea.innosistemas.team.dto.TeamResponseDTO;
import co.udea.innosistemas.team.dto.TeamUpdateRequestDTO;
import co.udea.innosistemas.team.model.Team;
import co.udea.innosistemas.team.model.TeamStatus;
import co.udea.innosistemas.team.repository.TeamRepository;
import co.udea.innosistemas.team.repository.TeamStatusRepository;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private static final int MAX_TEAM_MEMBERS = 3;
    private static final int MIN_TEAM_MEMBERS = 2;
    private static final int FORMATION_STATUS_ID = 1;
    private static final int INCOMPLETE_STATUS_ID = 2;
    private static final int EMPTY_STATUS_ID = 3;

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
        TeamStatus status = determineTeamStatus(users.size());
        Team team = createTeamEntity(request, status, creator);
        Team savedTeam = teamRepository.save(team);

        // Update user associations
        updateUserTeamAssociations(users, savedTeam);

        // Build and return response
        return buildTeamResponse(savedTeam, creator);
    }

    @Transactional
    public TeamResponseDTO updateTeam(Integer teamId, TeamUpdateRequestDTO request, User user) {
        if (teamId == null) {
            throw new IllegalArgumentException("Team ID cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Team update request cannot be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Find team
        Team team = findTeamById(teamId);

        // Check if user can modify this team (member of the team OR trying to join)
        boolean isTeamMember = user.getTeam() != null && user.getTeam().getId().equals(team.getId());
        boolean isTryingToJoin = !isTeamMember && !CollectionUtils.isEmpty(request.getAddUserIds())
                && request.getAddUserIds().contains(Long.valueOf(user.getId()));

        if (!isTeamMember && !isTryingToJoin) {
            throw new IllegalArgumentException("User is not authorized to modify this team");
        }

        // Get current team members
        List<User> currentMembers = getCurrentTeamMembers(team);

        // Process member updates using additive/subtractive approach
        Set<Long> newMemberIds = processTeamMemberUpdatesImproved(request, currentMembers, team.getCreator());

        // Validate final team composition
        validateTeamSize(newMemberIds);
        List<User> newMembers = fetchAndValidateUsers(newMemberIds);
        validateUsersTeamStatus(newMembers, team);

        // Update team properties
        updateTeamProperties(team, request, newMembers.size());

        // Update member associations
        updateTeamMemberAssociations(currentMembers, newMembers, team);

        Team savedTeam = teamRepository.save(team);
        log.info("Team {} updated successfully", teamId);

        return buildTeamResponse(savedTeam, team.getCreator());
    }

    @Transactional
    public TeamResponseDTO joinTeam(Integer teamId, User user) {
        if (teamId == null) {
            throw new IllegalArgumentException("Team ID cannot be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getTeam() != null) {
            throw new IllegalArgumentException("User already belongs to a team");
        }

        Team team = findTeamById(teamId);
        List<User> currentMembers = getCurrentTeamMembers(team);

        if (currentMembers.size() >= MAX_TEAM_MEMBERS) {
            throw new IllegalArgumentException("Team is already full");
        }

        // Add user to team
        user.setTeam(team);
        userRepository.save(user);

        // Update team status based on new member count
        updateTeamStatusBasedOnSize(team, currentMembers.size() + 1);
        Team savedTeam = teamRepository.save(team);

        log.info("User {} joined team {}", user.getEmail(), teamId);

        return buildTeamResponse(savedTeam, team.getCreator());
    }

    public TeamResponseDTO getTeamPublic(Integer teamId) {
        Team team = findTeamById(teamId);
        return buildTeamResponse(team, team.getCreator());
    }

    public TeamResponseDTO getMyTeam(User user) {
        if (user.getTeam() == null) {
            throw new IllegalArgumentException("User is not part of any team");
        }
        return getTeamPublic(user.getTeam().getId());
    }

    public List<TeamResponseDTO> getAvailableTeams() {
        // Obtener todos los equipos que no están completos
        List<Team> allTeams = teamRepository.findAll();

        return allTeams.stream()
                .filter(team -> {
                    List<User> members = getCurrentTeamMembers(team);
                    return members.size() < MAX_TEAM_MEMBERS;
                })
                .map(team -> buildTeamResponse(team, team.getCreator()))
                .collect(Collectors.toList());
    }

    public TeamResponseDTO getTeam(Integer teamId, User user) {
        Team team = findTeamById(teamId);
        validateUserCanViewTeam(user, team);

        return buildTeamResponse(team, team.getCreator());
    }

    public List<TeamResponseDTO> getUserTeams(User user) {
        if (user.getTeam() != null) {
            TeamResponseDTO teamResponse = getTeamPublic(user.getTeam().getId());
            return List.of(teamResponse);
        }
        return Collections.emptyList();
    }

    @Transactional
    public void leaveTeam(Integer teamId, User user) {
        Team team = findTeamById(teamId);
        validateUserBelongsToTeam(user, team);

        // El creador no puede abandonar el equipo
        if (team.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Team creator cannot leave the team");
        }

        List<User> currentMembers = getCurrentTeamMembers(team);

        // Remove user from team
        user.setTeam(null);
        userRepository.save(user);

        // Update remaining members and team status
        List<User> remainingMembers = currentMembers.stream()
                .filter(member -> !member.getId().equals(user.getId()))
                .collect(Collectors.toList());

        updateTeamStatusBasedOnSize(team, remainingMembers.size());
        teamRepository.save(team);

        log.info("User {} left team {}", user.getEmail(), teamId);
    }

    // Private helper methods
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
        validateUsersTeamStatus(users, null);
    }

    private void validateUsersTeamStatus(List<User> users, Team excludeTeam) {
        List<User> usersInTeam = users.stream()
                .filter(user -> user.getTeam() != null &&
                        (excludeTeam == null || !user.getTeam().getId().equals(excludeTeam.getId())))
                .collect(Collectors.toList());

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

    private TeamStatus determineTeamStatus(int memberCount) {
        if (memberCount >= MIN_TEAM_MEMBERS) {
            return getTeamStatusById(FORMATION_STATUS_ID);
        } else if (memberCount == 1) {
            return getTeamStatusById(INCOMPLETE_STATUS_ID);
        } else {
            return getTeamStatusById(EMPTY_STATUS_ID);
        }
    }

    private TeamStatus getTeamStatusById(int statusId) {
        return teamStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalStateException("Team status not found: " + statusId));
    }

    private Team createTeamEntity(TeamCreateRequestDTO request, TeamStatus status, User creator) {
        OffsetDateTime now = OffsetDateTime.now();
        return Team.builder()
                .name(request.getName())
                .status(status)
                .creator(creator)
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

    private Team findTeamById(Integer teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));
    }

    private void validateUserCanModifyTeam(User user, Team team) {
        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("User is not authorized to modify this team");
        }
    }

    private void validateUserCanViewTeam(User user, Team team) {
        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("User is not authorized to view this team");
        }
    }

    private void validateUserBelongsToTeam(User user, Team team) {
        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("User does not belong to this team");
        }
    }

    private List<User> getCurrentTeamMembers(Team team) {
        return userRepository.findByTeam(team);
    }

    private User findTeamCreator(List<User> members) {
        // Como no tenemos un campo createdAt en User, usaremos el ID más bajo como aproximación
        // Alternativamente, podrías agregar un campo 'isCreator' o 'createdBy' en el modelo User/Team
        // O almacenar el creatorId en la tabla Team
        return members.stream()
                .min(Comparator.comparing(User::getId))
                .orElseThrow(() -> new IllegalStateException("No creator found for team"));
    }

    private Set<Long> processTeamMemberUpdatesImproved(TeamUpdateRequestDTO request, List<User> currentMembers, User creator) {
        // Empezar con los miembros actuales
        Set<Long> memberIds = currentMembers.stream()
                .map(user -> Long.valueOf(user.getId()))
                .collect(Collectors.toSet());

        // Agregar nuevos miembros si se especificaron
        if (!CollectionUtils.isEmpty(request.getAddUserIds())) {
            memberIds.addAll(request.getAddUserIds());
        }

        // Remover miembros especificados (excepto el creador)
        if (!CollectionUtils.isEmpty(request.getRemoveUserIds())) {
            Set<Long> removeIds = new HashSet<>(request.getRemoveUserIds());
            removeIds.remove(Long.valueOf(creator.getId())); // No se puede remover al creador
            memberIds.removeAll(removeIds);
        }

        return memberIds;
    }

    private void updateTeamProperties(Team team, TeamUpdateRequestDTO request, int memberCount) {
        boolean updated = false;

        if (StringUtils.hasText(request.getName())) {
            team.setName(request.getName());
            updated = true;
        }

        if (request.getStatusId() != null) {
            TeamStatus status = getTeamStatusById(request.getStatusId());
            team.setStatus(status);
            updated = true;
        } else {
            // Auto-update status based on member count
            TeamStatus newStatus = determineTeamStatus(memberCount);
            if (!team.getStatus().getId().equals(newStatus.getId())) {
                team.setStatus(newStatus);
                updated = true;
            }
        }

        if (updated) {
            team.setUpdatedAt(OffsetDateTime.now());
        }
    }

    private void updateTeamMemberAssociations(List<User> currentMembers, List<User> newMembers, Team team) {
        // Remove team association from users who are no longer members
        Set<Long> newMemberIds = newMembers.stream()
                .map(user -> Long.valueOf(user.getId()))
                .collect(Collectors.toSet());

        List<User> usersToRemove = currentMembers.stream()
                .filter(user -> !newMemberIds.contains(Long.valueOf(user.getId())))
                .collect(Collectors.toList());

        usersToRemove.forEach(user -> user.setTeam(null));

        // Add team association to new members
        newMembers.forEach(user -> user.setTeam(team));

        // Save all affected users
        List<User> allAffectedUsers = new ArrayList<>();
        allAffectedUsers.addAll(usersToRemove);
        allAffectedUsers.addAll(newMembers);

        userRepository.saveAll(allAffectedUsers);
    }

    private void updateTeamStatusBasedOnSize(Team team, int memberCount) {
        TeamStatus newStatus = determineTeamStatus(memberCount);
        if (!team.getStatus().getId().equals(newStatus.getId())) {
            team.setStatus(newStatus);
            team.setUpdatedAt(OffsetDateTime.now());
        }
    }
}