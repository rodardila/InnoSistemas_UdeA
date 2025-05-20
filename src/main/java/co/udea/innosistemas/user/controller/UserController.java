package co.udea.innosistemas.user.controller;

import co.udea.innosistemas.user.dto.UserRegistrationRequestDTO;
import co.udea.innosistemas.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<String> register(@RequestBody @Valid UserRegistrationRequestDTO request) {
        userService.registerUser(request);
        return ResponseEntity.ok("Usuario registrado correctamente");
    }

    @GetMapping("/get-users")
    public ResponseEntity<?> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }
}