
package com.cloudtask.api;

import com.cloudtask.api.dto.LoginRequest;
import com.cloudtask.api.dto.LoginResponse;
import com.cloudtask.api.entity.RefreshToken;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.exception.InvalidRefreshTokenException;
import com.cloudtask.api.repository.RefreshTokenRepository;
import com.cloudtask.api.repository.UserRepository;
import com.cloudtask.api.service.AuthenticationService;
import com.cloudtask.api.service.JwtService;
import com.cloudtask.api.service.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                authenticationManager,
                jwtService,
                refreshTokenService,
                userRepository,
                refreshTokenRepository,
                900000L,
                604800000L
        );
    }

    @Test
    void loginShouldReturnTokensForValidCredentials() {

        Authentication authentication = mock(Authentication.class);

        User user = mock(User.class);

        when(authentication.getName())
                .thenReturn("test@cloudtask.com");

        when(authenticationManager.authenticate(
                any(Authentication.class)))
                .thenReturn(authentication);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken("test@cloudtask.com"))
                .thenReturn("access-token");

        when(refreshTokenService.generateRefreshToken())
                .thenReturn("refresh-token");

        when(refreshTokenService.hashToken("refresh-token"))
                .thenReturn("hashed-refresh-token");

        LoginRequest request = new LoginRequest(
                "test@cloudtask.com",
                "Password123"
        );

        LoginResponse response =
                authenticationService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        verify(refreshTokenRepository, times(1))
                .save(any(RefreshToken.class));
    }

    @Test
    void loginShouldRejectInvalidCredentials() {

        LoginRequest request = new LoginRequest(
                "test@cloudtask.com",
                "WrongPassword"
        );

        when(authenticationManager.authenticate(
                any(Authentication.class)))
                .thenThrow(
                        new BadCredentialsException("Invalid credentials")
                );

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(request)
        );

        verify(jwtService, never())
                .generateAccessToken(any());

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    void refreshShouldRevokeOldTokenAndCreateReplacement() {

        User user = mock(User.class);

        // The user must be enabled to refresh tokens.
        when(user.isEnabled()).thenReturn(true);

        when(user.getEmail())
                .thenReturn("test@cloudtask.com");

        RefreshToken oldToken = new RefreshToken(
                user,
                "old-token-hash",
                Instant.now().plusSeconds(3600)
        );

        when(refreshTokenService.validateAndGetToken("old-refresh-token"))
                .thenReturn(oldToken);

        when(jwtService.generateAccessToken("test@cloudtask.com"))
                .thenReturn("new-access-token");

        when(refreshTokenService.generateRefreshToken())
                .thenReturn("new-refresh-token");

        when(refreshTokenService.hashToken("new-refresh-token"))
                .thenReturn("new-token-hash");

        LoginResponse response =
                authenticationService.refresh("old-refresh-token");

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        assertTrue(oldToken.isRevoked());
        assertNotNull(oldToken.getReplacedBy());

        verify(refreshTokenRepository, times(2))
                .save(any(RefreshToken.class));
    }

    @Test
    void refreshShouldRejectInvalidRefreshToken() {

        when(refreshTokenService.validateAndGetToken("invalid-token"))
                .thenThrow(
                        new IllegalArgumentException("Invalid refresh token")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.refresh("invalid-token")
        );

        verify(jwtService, never())
                .generateAccessToken(any());

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    void refreshShouldRejectDisabledUser() {

        User user = mock(User.class);

        when(user.isEnabled()).thenReturn(false);

        RefreshToken oldToken = new RefreshToken(
                user,
                "old-token-hash",
                Instant.now().plusSeconds(3600)
        );

        when(refreshTokenService.validateAndGetToken("old-refresh-token"))
                .thenReturn(oldToken);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authenticationService.refresh("old-refresh-token")
        );

        // No new tokens should be generated for a disabled user.
        verify(jwtService, never())
                .generateAccessToken(any());

        verify(refreshTokenService, never())
                .generateRefreshToken();

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    void logoutShouldRevokeRefreshToken() {
        RefreshToken token = mock(RefreshToken.class);

        when(refreshTokenService.validateAndGetToken("refresh-token"))
                .thenReturn(token);

        authenticationService.logout("refresh-token");

        verify(refreshTokenService).validateAndGetToken("refresh-token");
        verify(refreshTokenService).revokeToken(token);
    }
}
