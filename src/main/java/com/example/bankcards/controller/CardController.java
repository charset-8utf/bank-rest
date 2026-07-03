package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Карты", description = "Операции с картами для авторизованного пользователя")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Получить свои карты (с пагинацией, опциональный фильтр по статусу)")
    public PageResponse<CardResponse> getMyCards(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) CardStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}) Pageable pageable) {
        return cardService.getMyCards(user.getId(), status, pageable);
    }

    @PutMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Запросить блокировку своей карты")
    public void requestBlock(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        cardService.requestBlock(id, user.getId());
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "История транзакций по своей карте (с пагинацией)")
    public PageResponse<TransactionResponse> getCardTransactions(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}) Pageable pageable) {
        return cardService.getCardTransactions(id, user.getId(), pageable);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Перевод средств между своими картами (обе карты должны принадлежать текущему пользователю)")
    public void transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User user) {
        cardService.transfer(request, user.getId());
    }
}