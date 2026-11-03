package com.cloudtask.api.service;

import com.cloudtask.api.dto.RegisterRequest;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.entity.UserRole;
import com.cloudtask.api.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered");
        }

        User user = new User(
        request.fullName().trim(),
        email,
        passwordEncoder.encode(request.password()),
        UserRole.USER
);

        return userRepository.save(user);
    }
}