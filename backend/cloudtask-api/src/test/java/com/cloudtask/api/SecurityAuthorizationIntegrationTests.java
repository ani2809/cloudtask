package com.cloudtask.api;

import com.cloudtask.api.config.SecurityConfig;
import com.cloudtask.api.security.JwtAuthenticationFilter;
import com.cloudtask.api.service.CustomUserDetailsService;
import com.cloudtask.api.service.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAuthorizationTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private void authenticateAs(String token, String email, String role) {
        UserDetails userDetails = User.withUsername(email)
                .password("password")
                .roles(role)
                .build();

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email))
                .thenReturn(userDetails);
    }

    @Test
    void adminEndpointShouldReturn401WhenJwtIsMissing() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void adminEndpointShouldReturn401WhenJwtIsInvalid() throws Exception {
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void adminEndpointShouldReturn403ForUserRole() throws Exception {
        authenticateAs("user-token", "user@test.com", "USER");

        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminEndpointShouldReturn403ForManagerRole() throws Exception {
        authenticateAs("manager-token", "manager@test.com", "MANAGER");

        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminEndpointShouldReturn200ForAdminRole() throws Exception {
        authenticateAs("admin-token", "admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin access granted"));
    }

    @Test
    void managerEndpointShouldReturn403ForUserRole() throws Exception {
        authenticateAs("user-token", "user@test.com", "USER");

        mockMvc.perform(get("/api/manager/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void managerEndpointShouldReturn200ForManagerRole() throws Exception {
        authenticateAs("manager-token", "manager@test.com", "MANAGER");

        mockMvc.perform(get("/api/manager/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer manager-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Manager access granted"));
    }
}
