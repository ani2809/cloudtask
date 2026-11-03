
package com.cloudtask.api.controller;

import com.cloudtask.api.dto.LoginRequest;
import com.cloudtask.api.dto.LoginResponse;
import com.cloudtask.api.dto.RegisterRequest;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.service.AuthService;
import com.cloudtask.api.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationService authenticationService;

    public AuthController(
            AuthService authService,
            AuthenticationService authenticationService) {

        this.authService = authService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        return Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authenticationService.login(request);
    }
}
