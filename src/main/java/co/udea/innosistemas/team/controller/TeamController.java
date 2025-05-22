package co.udea.innosistemas.team.controller;

import co.udea.innosistemas.team.dto.TeamCreateRequestDTO;
import co.udea.innosistemas.team.dto.TeamResponseDTO;
import co.udea.innosistemas.team.service.TeamService;
import co.udea.innosistemas.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Team Management", description = "APIs for team operations")
public class TeamController {

    private final TeamService teamService;

    @Operation(
        summary = "Create a new team",
        description = "Creates a new team with the specified details."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Team created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input provided"),
        @ApiResponse(responseCode = "403", description = "User not authorized to create teams"),
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