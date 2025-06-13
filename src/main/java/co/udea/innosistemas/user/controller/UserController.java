package co.udea.innosistemas.user.controller;

import co.udea.innosistemas.team.dto.TeamResponseDTO;
import co.udea.innosistemas.user.dto.UserRegisterResponseDTO;
import co.udea.innosistemas.user.dto.UserRegistrationRequestDTO;
import co.udea.innosistemas.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserRegisterResponseDTO> register(@RequestBody @Valid UserRegistrationRequestDTO request) {

        UserRegisterResponseDTO response = userService.registerUser(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-users")
    @SecurityRequirement(name = "bearerAuth")
    //public ResponseEntity<?> listUsers(@PageableDefault(size = 20) Pageable pageable) {
    public ResponseEntity<?> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }
}