package com.pyrosense.identity.adapter.in.rest;

import com.pyrosense.identity.application.port.in.RegisterUserUseCase;
import com.pyrosense.identity.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.pyrosense.identity.application.port.out.UserRepository;
import com.pyrosense.identity.config.Audited;
import com.pyrosense.identity.domain.model.User;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;

    public UserController(RegisterUserUseCase registerUserUseCase, UserRepository userRepository) {
        this.registerUserUseCase = registerUserUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "USER_CREATE", resourceType = "User")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.email(), request.fullName(),
                new TenantId(UUID.fromString(request.tenantId())), request.roles());
        User user = registerUserUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'SUPPORT_READONLY')")
    public ResponseEntity<UserResponse> getById(@PathVariable String userId) {
        return userRepository.findById(new UserId(UUID.fromString(userId)))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'SUPPORT_READONLY')")
    public ResponseEntity<List<UserResponse>> listByTenant(@RequestParam String tenantId) {
        List<UserResponse> users = userRepository.findByTenantId(new TenantId(UUID.fromString(tenantId)))
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(users);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().value().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank String fullName,
            @NotBlank String tenantId,
            @NotNull Set<String> roles
    ) {}

    record UserResponse(String id, String email, String fullName, String status,
                         Instant createdAt, Instant lastLoginAt) {}
}
