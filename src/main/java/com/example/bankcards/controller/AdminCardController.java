package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Admin — Карты", description = "Управление картами (только ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать карту для пользователя")
    public CardResponse createCard(@Valid @RequestBody CardCreateRequest request) {
        return cardService.createCard(request);
    }

    @GetMapping
    @Operation(summary = "Получить все карты (с пагинацией, опциональный фильтр по статусу)")
    public PageResponse<CardResponse> getAllCards(
            @RequestParam(required = false) CardStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}) Pageable pageable) {
        return cardService.getAllCards(status, pageable);
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Заблокировать карту")
    public CardResponse blockCard(@PathVariable Long id) {
        return cardService.blockCard(id);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Активировать карту")
    public CardResponse activateCard(@PathVariable Long id) {
        return cardService.activateCard(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить карту")
    public void deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
    }
}