package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCardControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean private CardService cardService;

    private User adminUser;
    private User regularUser;
    private CardResponse cardResponse;

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

        cardResponse = new CardResponse(1L, "**** **** **** 1234", "alice",
                LocalDate.now().plusYears(2), CardStatus.ACTIVE, BigDecimal.valueOf(1000), LocalDateTime.now());
    }

    @Test
    void createCard_asAdmin_returns201() throws Exception {
        CardCreateRequest request = new CardCreateRequest(2L, "4111111111111111", LocalDate.now().plusYears(2));
        when(cardService.createCard(any())).thenReturn(cardResponse);

        mockMvc.perform(post("/api/admin/cards")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void createCard_asUser_returns403() throws Exception {
        CardCreateRequest request = new CardCreateRequest(2L, "4111111111111111", LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/admin/cards")
                        .with(user(regularUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_unauthenticated_returns401() throws Exception {
        CardCreateRequest request = new CardCreateRequest(2L, "4111111111111111", LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCards_asAdmin_returns200() throws Exception {
        PageResponse<CardResponse> page = new PageResponse<>(List.of(cardResponse), 0, 20, 1L, 1, true);
        when(cardService.getAllCards(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/cards").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void blockCard_asAdmin_returns200() throws Exception {
        CardResponse blocked = new CardResponse(1L, "**** **** **** 1234", "alice",
                LocalDate.now().plusYears(2), CardStatus.BLOCKED, BigDecimal.valueOf(1000), LocalDateTime.now());
        when(cardService.blockCard(1L)).thenReturn(blocked);

        mockMvc.perform(put("/api/admin/cards/1/block").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void activateCard_asAdmin_returns200() throws Exception {
        when(cardService.activateCard(1L)).thenReturn(cardResponse);

        mockMvc.perform(put("/api/admin/cards/1/activate").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteCard_asAdmin_returns204() throws Exception {
        doNothing().when(cardService).deleteCard(1L);

        mockMvc.perform(delete("/api/admin/cards/1").with(user(adminUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCard_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Карта не найдена: 99"))
                .when(cardService).deleteCard(99L);

        mockMvc.perform(delete("/api/admin/cards/99").with(user(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_invalidCardNumberFormat_returns400() throws Exception {
        CardCreateRequest request = new CardCreateRequest(2L, "1234-INVALID-NUM", LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/admin/cards")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_pastExpiryDate_returns400() throws Exception {
        CardCreateRequest request = new CardCreateRequest(2L, "4111111111111111", LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/admin/cards")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
