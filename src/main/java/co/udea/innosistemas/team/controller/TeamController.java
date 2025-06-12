package co.udea.innosistemas.team.controller;

import co.udea.innosistemas.team.dto.TeamCreateRequestDTO;
import co.udea.innosistemas.team.dto.TeamResponseDTO;
import co.udea.innosistemas.team.dto.TeamUpdateRequestDTO;
import co.udea.innosistemas.team.service.TeamService;
import co.udea.innosistemas.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Team Management", description = "APIs for team operations")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;

    @Operation(
            summary = "Create a new team",
            description = "Creates a new team with the specified details. Available for all authenticated users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input provided"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
            @RequestBody @Valid TeamCreateRequestDTO request,
            Authentication authentication) {

        log.debug("Received request to create team: {}", request.getName());

        User creator = validateAndGetUser(authentication);
        log.info("User {} attempting to create team", creator.getEmail());

        TeamResponseDTO response = teamService.createTeam(request, creator);

        log.info("Team successfully created with ID: {}", response.getId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update an existing team",
            description = "Updates an existing team. Only administrators can modify teams."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input provided"),
            @ApiResponse(responseCode = "403", description = "User not authorized to update teams"),
            @ApiResponse(responseCode = "404", description = "Team not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponseDTO> updateTeam(
            @PathVariable Integer teamId,
            @RequestBody @Valid TeamUpdateRequestDTO request,
            Authentication authentication) {

        log.debug("Received admin request to update team ID: {}", teamId);

        User admin = validateAndGetUser(authentication);
        log.info("Admin {} attempting to update team ID: {}", admin.getEmail(), teamId);

        TeamResponseDTO response = teamService.updateTeam(teamId, request, admin);

        log.info("Team successfully updated with ID: {}", response.getId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Join a team",
            description = "Allows a student to join an existing team if there are available spots"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined the team"),
            @ApiResponse(responseCode = "400", description = "Team is full or user already has a team"),
            @ApiResponse(responseCode = "404", description = "Team not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{teamId}/join")
    public ResponseEntity<TeamResponseDTO> joinTeam(
            @PathVariable Integer teamId,
            Authentication authentication) {

        log.debug("Received request to join team ID: {}", teamId);

        User user = validateAndGetUser(authentication);
        log.info("User {} attempting to join team ID: {}", user.getEmail(), teamId);

        TeamResponseDTO response = teamService.joinTeam(teamId, user);

        log.info("User {} successfully joined team ID: {}", user.getEmail(), teamId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get team details",
            description = "Retrieves details of a specific team"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Team not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponseDTO> getTeam(@PathVariable Integer teamId) {

        log.debug("Received request to get team ID: {}", teamId);

        TeamResponseDTO response = teamService.getTeamPublic(teamId);

        log.info("Team details retrieved for ID: {}", teamId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all available teams",
            description = "Retrieves all teams that have available spots for new members, including current member count and available spots"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available teams retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/available")
    public ResponseEntity<List<TeamResponseDTO>> getAvailableTeams() {

        log.debug("Received request to get available teams");

        List<TeamResponseDTO> response = teamService.getAvailableTeams();

        log.info("Retrieved {} available teams", response.size());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get current user's team",
            description = "Retrieves the team where the current user is a member"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User team retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User is not part of any team"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-team")
    public ResponseEntity<TeamResponseDTO> getMyTeam(Authentication authentication) {

        log.debug("Received request to get user's team");

        User user = validateAndGetUser(authentication);
        log.info("User {} requesting their team", user.getEmail());

        TeamResponseDTO response = teamService.getMyTeam(user);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Leave a team",
            description = "Allows any user to leave their current team, including the original creator. The creator record is preserved for historical purposes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully left the team"),
            @ApiResponse(responseCode = "404", description = "User is not part of any team"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{teamId}/leave")
    public ResponseEntity<Void> leaveTeam(
            @PathVariable Integer teamId,
            Authentication authentication) {

        log.debug("Received request to leave team ID: {}", teamId);

        User user = validateAndGetUser(authentication);
        log.info("User {} attempting to leave team ID: {}", user.getEmail(), teamId);

        teamService.leaveTeam(teamId, user);

        log.info("User {} successfully left team ID: {}", user.getEmail(), teamId);

        return ResponseEntity.ok().build();
    }

    private User validateAndGetUser(Authentication authentication) {
        if (authentication == null) {
            log.error("Authentication object is null");
            throw new SecurityException("No authentication present");
        }

        if (!(authentication.getPrincipal() instanceof User)) {
            log.error("Principal is not of type User: {}",
                    authentication.getPrincipal().getClass());
            throw new SecurityException("Invalid authentication principal");
        }

        User user = (User) authentication.getPrincipal();
        log.debug("Validated user: {}", user.getEmail());
        return user;
    }
}