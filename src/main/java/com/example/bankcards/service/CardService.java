package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.entity.CardStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;

public interface CardService {

    CardResponse createCard(CardCreateRequest request);

    CardResponse blockCard(Long cardId);

    CardResponse activateCard(Long cardId);

    void deleteCard(Long cardId);

    PageResponse<CardResponse> getAllCards(@Nullable CardStatus status, Pageable pageable);

    PageResponse<CardResponse> getMyCards(Long userId, @Nullable CardStatus status, Pageable pageable);

    PageResponse<TransactionResponse> getCardTransactions(Long cardId, Long userId, Pageable pageable);

    void requestBlock(Long cardId, Long userId);

    void transfer(TransferRequest request, Long userId);
}
