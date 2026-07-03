package com.example.bankcards.controller;

import com.example.bankcards.dto.request.UserProfileRequest;
import com.example.bankcards.dto.response.UserProfileResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private UserService userService;

    private User testUser;
    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .password("hashed")
                .roles(new HashSet<>(List.of(Role.builder().name(RoleType.USER).build())))
                .build();

        profileResponse = new UserProfileResponse(1L, "alice", "alice@test.com", "Alice", "Smith", "+71234567890");
    }

    @Test
    void getMyProfile_authenticated_returns200() throws Exception {
        when(userService.getMyProfile(1L)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/profile").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.phone").value("+71234567890"));
    }

    @Test
    void getMyProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyProfile_valid_returns200() throws Exception {
        UserProfileRequest request = new UserProfileRequest("Bob", "Jones", "+79998887766");
        UserProfileResponse updated = new UserProfileResponse(1L, "alice", "alice@test.com", "Bob", "Jones", "+79998887766");
        when(userService.updateMyProfile(eq(1L), any(UserProfileRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/profile")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.lastName").value("Jones"));
    }

    @Test
    void updateMyProfile_invalidPhone_returns400() throws Exception {
        UserProfileRequest request = new UserProfileRequest("Bob", "Jones", "not-a-phone!!!");

        mockMvc.perform(put("/api/users/profile")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
