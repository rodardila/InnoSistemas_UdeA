package co.udea.innosistemas.user.controller;

import co.udea.innosistemas.common.dto.ApiError;
import co.udea.innosistemas.user.dto.UserRegisterResponseDTO;
import co.udea.innosistemas.user.dto.UserRegistrationRequestDTO;
import co.udea.innosistemas.user.dto.UserResponseDTO;
import co.udea.innosistemas.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user operations")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided information. Email must be from @udea.edu.co domain."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data provided"),
            @ApiResponse(responseCode = "409", description = "User already exists (duplicate email or document)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<UserRegisterResponseDTO> register(
            @RequestBody @Valid UserRegistrationRequestDTO request) {

        log.debug("Received user registration request for email: {}", request.getEmail());

        UserRegisterResponseDTO response = userService.registerUser(request);

        log.info("User registration successful for email: {}", response.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List all users",
            description = "Retrieves a paginated list of all users in the system. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/get-users")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<UserResponseDTO>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Received request to list users - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<UserResponseDTO> users = userService.listUsers(pageable);

        log.info("Retrieved {} users (page {} of {})",
                users.getNumberOfElements(),
                users.getNumber() + 1,
                users.getTotalPages());

        return ResponseEntity.ok(users);
    }

}