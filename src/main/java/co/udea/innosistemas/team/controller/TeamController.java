package co.udea.innosistemas.team.controller;

import co.udea.innosistemas.team.DTO.TeamCreateRequest;
import co.udea.innosistemas.team.service.TeamService;
import co.udea.innosistemas.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<String> createTeam(@RequestBody @Valid TeamCreateRequest request,
                                             Authentication authentication) {
        User creator = (User) authentication.getPrincipal();  // Accede al usuario autenticado
        teamService.createTeam(request, creator);
        return ResponseEntity.ok("Equipo creado correctamente");
    }
}
