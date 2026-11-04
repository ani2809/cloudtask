
package com.cloudtask.api;

import com.cloudtask.api.config.SecurityConfig;
import com.cloudtask.api.controller.AuthController;
import com.cloudtask.api.dto.LoginRequest;
import com.cloudtask.api.dto.LoginResponse;
import com.cloudtask.api.dto.RegisterRequest;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.entity.UserRole;
import com.cloudtask.api.exception.InvalidRefreshTokenException;
import com.cloudtask.api.security.JwtAuthenticationFilter;
import com.cloudtask.api.service.AuthService;
import com.cloudtask.api.service.AuthenticationService;
import com.cloudtask.api.service.CustomUserDetailsService;
import com.cloudtask.api.service.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LoginResponse loginResponse() {
        return new LoginResponse(
                "test-access-token",
                "test-refresh-token",
                "Bearer",
                900
        );
    }

    @Test
    void registerShouldReturnCreated() throws Exception {

        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(user.getFullName()).thenReturn("Test User");
        when(user.getEmail()).thenReturn("test@cloudtask.com");
        when(user.getRole()).thenReturn(UserRole.USER);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Test User",
                                  "email": "test@cloudtask.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@cloudtask.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registerShouldRejectInvalidRequest() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "invalid-email",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void loginShouldReturnTokens() throws Exception {

        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(loginResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@cloudtask.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("test-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void loginShouldRejectInvalidRequest() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never())
                .login(any(LoginRequest.class));
    }

    @Test
    void loginShouldRejectIncorrectCredentials() throws Exception {

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@cloudtask.com",
                                  "password": "WrongPassword123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(authenticationService, times(1))
                .login(any(LoginRequest.class));
    }

    @Test
    void refreshShouldReturnNewTokens() throws Exception {

        when(authenticationService.refresh("test-refresh-token"))
                .thenReturn(loginResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "test-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("test-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void refreshShouldRejectInvalidRefreshToken() throws Exception {

        when(authenticationService.refresh("invalid-refresh-token"))
                .thenThrow(new InvalidRefreshTokenException(
                        "Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-refresh-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        verify(authenticationService, times(1))
                .refresh("invalid-refresh-token");
    }

    @Test
    void refreshShouldRejectEmptyRefreshToken() throws Exception {

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never())
                .refresh(anyString());
    }

    @Test
    void logoutShouldRevokeRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "test-refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authenticationService).logout("test-refresh-token");
    }
}
