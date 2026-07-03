package com.example.bankcards.integration;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardLifecycleIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;
    private String userToken;
    private Long card1Id;
    private Long card2Id;

    @BeforeEach
    void setUp() throws Exception {
        cleanDb();

        adminToken = register("admin", "admin@test.com");
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT u.id, r.id FROM users u, roles r
                WHERE u.username = 'admin' AND r.name = 'ADMIN'
                """);

        userToken = register("alice", "alice@test.com");
        Long aliceId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'alice'", Long.class);

        card1Id = createCard(Objects.requireNonNull(aliceId), "4111111111111111");
        card2Id = createCard(aliceId, "5500005555555559");

        jdbcTemplate.update("UPDATE cards SET balance = 1000.00 WHERE id = ?", card1Id);
    }

    @Test
    void getMyCards_asUser_returnsBothCards() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void blockAndActivateCard_asAdmin_changesStatus() throws Exception {
        mockMvc.perform(put("/api/admin/cards/" + card1Id + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CardStatus.BLOCKED.name()));

        mockMvc.perform(put("/api/admin/cards/" + card1Id + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CardStatus.ACTIVE.name()));
    }

    @Test
    void transfer_betweenOwnCards_updatesBalancesInDb() throws Exception {
        mockMvc.perform(post("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequest(card1Id, card2Id, BigDecimal.valueOf(300)))))
                .andExpect(status().isNoContent());

        BigDecimal fromBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM cards WHERE id = ?", BigDecimal.class, card1Id);
        BigDecimal toBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM cards WHERE id = ?", BigDecimal.class, card2Id);

        assertThat(fromBalance).isEqualByComparingTo("700.00");
        assertThat(toBalance).isEqualByComparingTo("300.00");
    }

    private Long createCard(Long ownerId, String cardNumber) throws Exception {
        String body = mockMvc.perform(post("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardCreateRequest(ownerId, cardNumber, LocalDate.now().plusYears(3)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
