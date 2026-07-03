package com.example.bankcards.controller;

import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;

    private User adminUser;
    private User regularUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .password("hashed")
                .roles(new HashSet<>(List.of(Role.builder().name(RoleType.ADMIN).build())))
                .build();

        regularUser = User.builder()
                .id(2L)
                .username("alice")
                .email("alice@test.com")
                .password("hashed")
                .roles(new HashSet<>(List.of(Role.builder().name(RoleType.USER).build())))
                .build();

        userResponse = new UserResponse(2L, "alice", "alice@test.com", Set.of("USER"), LocalDateTime.now());
    }

    @Test
    void getAllUsers_asAdmin_returns200() throws Exception {
        PageResponse<UserResponse> page = new PageResponse<>(List.of(userResponse), 0, 20, 1L, 1, true);
        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getUserById_asAdmin_returns200() throws Exception {
        when(userService.getUserById(2L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/admin/users/2").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("Пользователь не найден: 99"));

        mockMvc.perform(get("/api/admin/users/99").with(user(adminUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь не найден: 99"));
    }

    @Test
    void deleteUser_asAdmin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/api/admin/users/2").with(user(adminUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
