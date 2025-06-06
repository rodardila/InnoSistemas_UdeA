package co.udea.innosistemas.team.service;

import co.udea.innosistemas.common.service.AuditService;
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

    // Estados de equipos:
    // 1 = En formación (1 miembro)
    // 2 = Incompleto (2 miembros)
    // 3 = Completo (3 miembros)
    // 4 = Vacío (0 miembros)
    private static final int EN_FORMACION_STATUS_ID = 1;    // 1 miembro
    private static final int INCOMPLETO_STATUS_ID = 2;      // 2 miembros
    private static final int COMPLETO_STATUS_ID = 3;        // 3 miembros
    private static final int VACIO_STATUS_ID = 4;           // 0 miembros

    private final TeamRepository teamRepository;
    private final TeamStatusRepository teamStatusRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public TeamResponseDTO createTeam(TeamCreateRequestDTO request, User creator) {
        validateCreateRequest(request, creator);

        // Initialize member IDs set with creator
        Set<Long> memberIds = initializeMemberIds(request, creator);
        validateTeamSize(memberIds);

        // Fetch and validate users
        List<User> users = fetchAndValidateUsers(memberIds);
        validateUsersTeamStatus(users);

        // Get team status based on initial member count and create team
        TeamStatus status = determineTeamStatus(users.size());
        Team team = createTeamEntity(request, status, creator);
        Team savedTeam = teamRepository.save(team);

        // Update user associations
        updateUserTeamAssociations(users, savedTeam);

        // Log team creation
        auditService.logTeamCreated(creator, savedTeam.getId(), savedTeam.getName());

        // Log member additions
        users.stream()
                .filter(user -> !user.getId().equals(creator.getId()))
                .forEach(user -> auditService.logMemberAdded(creator, user, savedTeam.getId(), savedTeam.getName()));

        log.info("Team created successfully with ID: {}", savedTeam.getId());
        return buildTeamResponse(savedTeam, creator);
    }

    @Transactional
    public TeamResponseDTO updateTeam(Integer teamId, TeamUpdateRequestDTO request, User user) {
        validateUpdateRequest(teamId, request, user);

        // Validar que solo administradores puedan modificar equipos
        if (!isAdmin(user)) {
            throw new IllegalArgumentException("Only administrators can modify teams");
        }

        Team team = findTeamById(teamId);
        List<User> currentMembers = getCurrentTeamMembers(team);

        // Procesar cambios de miembros
        TeamMemberChanges changes = processTeamMemberUpdatesWithChanges(request, currentMembers, team.getCreator());

        // Validar composición final
        validateTeamSize(changes.getFinalMemberIds());
        List<User> newMembers = fetchAndValidateUsers(changes.getFinalMemberIds());
        validateUsersTeamStatus(newMembers, team);

        // Actualizar propiedades del equipo
        String teamChanges = updateTeamProperties(team, request, newMembers.size());

        // Actualizar asociaciones de miembros
        updateTeamMemberAssociations(currentMembers, newMembers, team);

        Team savedTeam = teamRepository.save(team);

        // Registrar auditoría
        auditService.logTeamUpdated(user, teamId, team.getName(), teamChanges);

        // Log member additions
        changes.getAddedUsers().forEach(addedUser ->
                auditService.logMemberAdded(user, addedUser, teamId, team.getName()));

        // Log member removals
        changes.getRemovedUsers().forEach(removedUser ->
                auditService.logMemberRemoved(user, removedUser, teamId, team.getName()));

        log.info("Team {} updated successfully by admin {}", teamId, user.getEmail());
        return buildTeamResponse(savedTeam, team.getCreator());
    }

    @Transactional
    public TeamResponseDTO joinTeam(Integer teamId, User user) {
        if (teamId == null || user == null) {
            throw new IllegalArgumentException("Team ID and user cannot be null");
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

        // Log user joining
        auditService.logUserJoinedTeam(user, teamId, team.getName());

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
        List<Team> allTeams = teamRepository.findAll();

        return allTeams.stream()
                .map(team -> {
                    List<User> members = getCurrentTeamMembers(team);
                    int currentMembers = members.size();
                    int availableSpots = MAX_TEAM_MEMBERS - currentMembers;

                    return TeamResponseDTO.builder()
                            .id(team.getId())
                            .name(team.getName())
                            .creatorEmail(team.getCreator().getEmail())
                            .createdAt(team.getCreatedAt().toLocalDateTime())
                            .currentMembers(currentMembers)
                            .availableSpots(availableSpots)
                            .build();
                })
                .filter(teamDto -> teamDto.getAvailableSpots() > 0) // Solo equipos con cupos disponibles
                .collect(Collectors.toList());
    }

    @Transactional
    public void leaveTeam(Integer teamId, User user) {
        Team team = findTeamById(teamId);
        validateUserBelongsToTeam(user, team);

        List<User> currentMembers = getCurrentTeamMembers(team);

        // Remove user from team (including if they are the creator)
        user.setTeam(null);
        userRepository.save(user);

        // Update team status based on remaining members
        updateTeamStatusBasedOnSize(team, currentMembers.size() - 1);
        teamRepository.save(team);

        // Log user leaving (with special note if it was the creator)
        if (team.getCreator().getId().equals(user.getId())) {
            auditService.logTeamEvent(user, "TEAM_CREATOR_LEFT",
                    String.format("Team creator left team '%s' (ID: %d). Creator record preserved for historical purposes.",
                            team.getName(), teamId));
        } else {
            auditService.logUserLeftTeam(user, teamId, team.getName());
        }

        log.info("User {} left team {} (was creator: {})",
                user.getEmail(), teamId, team.getCreator().getId().equals(user.getId()));
    }

    // Validation methods
    private void validateCreateRequest(TeamCreateRequestDTO request, User creator) {
        if (request == null) {
            throw new IllegalArgumentException("Team creation request cannot be null");
        }
        if (creator == null) {
            throw new IllegalArgumentException("Team creator cannot be null");
        }
    }

    private void validateUpdateRequest(Integer teamId, TeamUpdateRequestDTO request, User user) {
        if (teamId == null) {
            throw new IllegalArgumentException("Team ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Team update request cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equals(user.getRole().getName().toUpperCase());
    }

    // Member management methods
    private Set<Long> initializeMemberIds(TeamCreateRequestDTO request, User creator) {
        Set<Long> memberIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.getUserIds())) {
            memberIds.addAll(request.getUserIds());
        }
        memberIds.add(Long.valueOf(creator.getId()));
        return memberIds;
    }

    private TeamMemberChanges processTeamMemberUpdatesWithChanges(TeamUpdateRequestDTO request,
                                                                  List<User> currentMembers,
                                                                  User creator) {
        Set<Long> currentMemberIds = currentMembers.stream()
                .map(user -> Long.valueOf(user.getId()))
                .collect(Collectors.toSet());

        Set<Long> finalMemberIds = new HashSet<>(currentMemberIds);

        List<User> addedUsers = new ArrayList<>();
        List<User> removedUsers = new ArrayList<>();

        // Add new members
        if (!CollectionUtils.isEmpty(request.getAddUserIds())) {
            Set<Long> usersToAdd = new HashSet<>(request.getAddUserIds());
            usersToAdd.removeAll(currentMemberIds); // Solo agregar usuarios que no estén ya

            finalMemberIds.addAll(usersToAdd);
            addedUsers = userRepository.findAllById(usersToAdd);
        }

        // Remove members (except creator)
        if (!CollectionUtils.isEmpty(request.getRemoveUserIds())) {
            Set<Long> usersToRemove = new HashSet<>(request.getRemoveUserIds());
            usersToRemove.remove(Long.valueOf(creator.getId())); // Can't remove creator
            usersToRemove.retainAll(currentMemberIds); // Solo remover usuarios que estén actualmente

            finalMemberIds.removeAll(usersToRemove);
            removedUsers = currentMembers.stream()
                    .filter(user -> usersToRemove.contains(Long.valueOf(user.getId())))
                    .collect(Collectors.toList());
        }

        return new TeamMemberChanges(finalMemberIds, addedUsers, removedUsers);
    }

    // Validation methods
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

    private void validateUserBelongsToTeam(User user, Team team) {
        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("User does not belong to this team");
        }
    }

    // Team entity management
    private Team findTeamById(Integer teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));
    }

    private List<User> getCurrentTeamMembers(Team team) {
        return userRepository.findByTeam(team);
    }

    private TeamStatus determineTeamStatus(int memberCount) {
        switch (memberCount) {
            case 0:
                return getTeamStatusById(VACIO_STATUS_ID);           // Sin integrantes
            case 1:
                return getTeamStatusById(EN_FORMACION_STATUS_ID);    // En formación
            case 2:
                return getTeamStatusById(INCOMPLETO_STATUS_ID);      // Incompleto
            case 3:
                return getTeamStatusById(COMPLETO_STATUS_ID);        // Completo
            default:
                throw new IllegalStateException("Invalid team member count: " + memberCount);
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

    private String updateTeamProperties(Team team, TeamUpdateRequestDTO request, int memberCount) {
        List<String> changes = new ArrayList<>();

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(team.getName())) {
            String oldName = team.getName();
            team.setName(request.getName());
            changes.add(String.format("Name: '%s' -> '%s'", oldName, request.getName()));
        }

        if (request.getStatusId() != null && !request.getStatusId().equals(team.getStatus().getId())) {
            TeamStatus oldStatus = team.getStatus();
            TeamStatus newStatus = getTeamStatusById(request.getStatusId());
            team.setStatus(newStatus);
            changes.add(String.format("Status: '%s' -> '%s'", oldStatus.getName(), newStatus.getName()));
        } else {
            // Auto-update status based on member count if not explicitly set
            TeamStatus newStatus = determineTeamStatus(memberCount);
            if (!team.getStatus().getId().equals(newStatus.getId())) {
                TeamStatus oldStatus = team.getStatus();
                team.setStatus(newStatus);
                changes.add(String.format("Status (auto): '%s' -> '%s'", oldStatus.getName(), newStatus.getName()));
            }
        }

        if (!changes.isEmpty()) {
            team.setUpdatedAt(OffsetDateTime.now());
        }

        return changes.isEmpty() ? "No property changes" : String.join(", ", changes);
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

    private void updateUserTeamAssociations(List<User> users, Team team) {
        users.forEach(user -> user.setTeam(team));
        userRepository.saveAll(users);
    }

    private void updateTeamStatusBasedOnSize(Team team, int memberCount) {
        TeamStatus newStatus = determineTeamStatus(memberCount);
        if (!team.getStatus().getId().equals(newStatus.getId())) {
            team.setStatus(newStatus);
            team.setUpdatedAt(OffsetDateTime.now());
        }
    }

    private TeamResponseDTO buildTeamResponse(Team team, User creator) {
        List<User> members = getCurrentTeamMembers(team);
        int currentMembers = members.size();
        int availableSpots = MAX_TEAM_MEMBERS - currentMembers;

        return TeamResponseDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .creatorEmail(creator.getEmail())
                .createdAt(team.getCreatedAt().toLocalDateTime())
                .currentMembers(currentMembers)
                .availableSpots(availableSpots)
                .build();
    }

    // Inner class for tracking member changes
    private static class TeamMemberChanges {
        private final Set<Long> finalMemberIds;
        private final List<User> addedUsers;
        private final List<User> removedUsers;

        public TeamMemberChanges(Set<Long> finalMemberIds, List<User> addedUsers, List<User> removedUsers) {
            this.finalMemberIds = finalMemberIds;
            this.addedUsers = addedUsers;
            this.removedUsers = removedUsers;
        }

        public Set<Long> getFinalMemberIds() { return finalMemberIds; }
        public List<User> getAddedUsers() { return addedUsers; }
        public List<User> getRemovedUsers() { return removedUsers; }
    }
}