
package com.cloudtask.api.service;

import com.cloudtask.api.dto.LoginRequest;
import com.cloudtask.api.dto.LoginResponse;
import com.cloudtask.api.entity.RefreshToken;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.exception.InvalidRefreshTokenException;
import com.cloudtask.api.repository.RefreshTokenRepository;
import com.cloudtask.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.access-token-expiration}")
            long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}")
            long refreshTokenExpiration) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email().trim().toLowerCase(),
                                request.password()
                        )
                );

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found")
                );

        String accessToken =
                jwtService.generateAccessToken(email);

        String refreshToken =
                refreshTokenService.generateRefreshToken();

        RefreshToken tokenEntity = new RefreshToken(
                user,
                refreshTokenService.hashToken(refreshToken),
                Instant.now().plusMillis(refreshTokenExpiration)
        );

        refreshTokenRepository.save(tokenEntity);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiration / 1000
        );
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {

        RefreshToken oldToken =
                refreshTokenService.validateAndGetToken(
                        rawRefreshToken);

        User user = oldToken.getUser();

        // Prevent disabled users from refreshing their tokens.
        if (!user.isEnabled()) {
            throw new InvalidRefreshTokenException(
                    "User account is disabled");
        }

        String email = user.getEmail();

        String newAccessToken =
                jwtService.generateAccessToken(email);

        String newRefreshToken =
                refreshTokenService.generateRefreshToken();

        RefreshToken replacementToken = new RefreshToken(
                user,
                refreshTokenService.hashToken(newRefreshToken),
                Instant.now().plusMillis(refreshTokenExpiration)
        );

        refreshTokenRepository.save(replacementToken);

        oldToken.setRevokedAt(Instant.now());
        oldToken.setReplacedBy(replacementToken);

        refreshTokenRepository.save(oldToken);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                accessTokenExpiration / 1000
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {

        RefreshToken refreshToken =
                refreshTokenService.validateAndGetToken(
                        rawRefreshToken);

        refreshTokenService.revokeToken(refreshToken);
    }
}
