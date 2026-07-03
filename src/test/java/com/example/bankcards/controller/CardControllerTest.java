package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.service.CardService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private CardService cardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .password("hashed")
                .roles(new java.util.HashSet<>(java.util.List.of(Role.builder().name(RoleType.USER).build())))
                .build();
    }

    @Test
    void getMyCards_authenticated_returns200() throws Exception {
        CardResponse card = new CardResponse(1L, "**** **** **** 1234", "alice",
                LocalDate.now().plusYears(2), CardStatus.ACTIVE, BigDecimal.valueOf(500), LocalDateTime.now());
        PageResponse<CardResponse> page = new PageResponse<>(List.of(card), 0, 20, 1L, 1, true);

        when(cardService.getMyCards(eq(1L), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/cards").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMyCards_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transfer_valid_returns204() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.valueOf(100));
        doNothing().when(cardService).transfer(any(), eq(1L));

        mockMvc.perform(post("/api/cards/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void transfer_insufficientFunds_returns400() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.valueOf(9999));
        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(cardService).transfer(any(), eq(1L));

        mockMvc.perform(post("/api/cards/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void transfer_zeroAmount_returns400() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.ZERO);

        mockMvc.perform(post("/api/cards/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestBlock_authenticated_returns204() throws Exception {
        doNothing().when(cardService).requestBlock(1L, 1L);

        mockMvc.perform(put("/api/cards/1/block").with(user(testUser)))
                .andExpect(status().isNoContent());
    }
}
