package com.example.bankcards.service;

import com.example.bankcards.config.CacheConfig;
import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.event.TransferCompletedEvent;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.transfer.TransferContext;
import com.example.bankcards.service.transfer.TransferHandler;
import com.example.bankcards.util.CardEncryptionStrategy;
import com.example.bankcards.util.CardMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionStrategy encryptionStrategy;
    private final CardMaskUtil cardMaskUtil;
    private final CardMapper cardMapper;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransferHandler transferValidationChain;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CARDS_CACHE, allEntries = true)
    public CardResponse createCard(CardCreateRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + request.ownerId()));

        Card card = Card.builder()
                .cardNumberEncrypted(encryptionStrategy.encrypt(request.cardNumber()))
                .owner(owner)
                .expiryDate(request.expiryDate())
                .status(CardStatus.ACTIVE)
                .build();

        Card saved = cardRepository.save(card);
        log.info("Карта создана: cardId={}, ownerId={}", saved.getId(), request.ownerId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CARDS_CACHE, allEntries = true)
    public CardResponse blockCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        card.setStatus(CardStatus.BLOCKED);
        log.info("Карта заблокирована: cardId={}", cardId);
        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CARDS_CACHE, allEntries = true)
    public CardResponse activateCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        card.getStatus().requireActivatable();
        card.setStatus(CardStatus.ACTIVE);
        log.info("Карта активирована: cardId={}", cardId);
        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CARDS_CACHE, allEntries = true)
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Карта не найдена: " + cardId);
        }
        cardRepository.deleteById(cardId);
        log.info("Карта удалена: cardId={}", cardId);
    }

    @Override
    public PageResponse<CardResponse> getAllCards(@Nullable CardStatus status, Pageable pageable) {
        Page<Card> page = (status != null)
                ? cardRepository.findByStatus(status, pageable)
                : cardRepository.findAll(pageable);
        return new PageResponse<>(page.map(this::toResponse));
    }

    @Override
    @Cacheable(value = CacheConfig.CARDS_CACHE,
            key = "#userId + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<CardResponse> getMyCards(Long userId, @Nullable CardStatus status, Pageable pageable) {
        Page<Card> page = (status != null)
                ? cardRepository.findByOwnerIdAndStatus(userId, status, pageable)
                : cardRepository.findByOwnerId(userId, pageable);
        return new PageResponse<>(page.map(this::toResponse));
    }

    @Override
    public PageResponse<TransactionResponse> getCardTransactions(Long cardId, Long userId, Pageable pageable) {
        Card card = getCardOrThrow(cardId);
        checkOwnership(card, userId);
        return new PageResponse<>(
                transactionRepository.findByCardId(cardId, pageable).map(this::toTransactionResponse)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CARDS_CACHE, allEntries = true)
    public void requestBlock(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkOwnership(card, userId);
        card.getStatus().requireActive();
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        log.info("Пользователь запросил блокировку: cardId={}, userId={}", cardId, userId);
    }

    @Override
    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    @CacheEvict(value = CacheConfig.CARDS_CACHE, allEntries = true)
    public void transfer(TransferRequest request, Long userId) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new IllegalArgumentException("Карта-источник и карта-получатель должны отличаться");
        }

        Card from = getCardOrThrow(request.fromCardId());
        Card to = getCardOrThrow(request.toCardId());

        transferValidationChain.handle(new TransferContext(from, to, request.amount(), userId));

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));

        cardRepository.save(from);
        cardRepository.save(to);

        eventPublisher.publishEvent(new TransferCompletedEvent(from, to, request.amount()));

        log.info("Перевод выполнен: fromCard={}, toCard={}, amount={}, userId={}",
                request.fromCardId(), request.toCardId(), request.amount(), userId);
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverTransfer(OptimisticLockingFailureException ex, TransferRequest request, Long userId) {
        log.error("Перевод не выполнен после 3 попыток: fromCard={}, toCard={}",
                request.fromCardId(), request.toCardId(), ex);
        throw new IllegalStateException("Перевод временно недоступен из-за конкурентного изменения данных. Попробуйте позже.");
    }

    private Card getCardOrThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + cardId));
    }

    private void checkOwnership(Card card, Long userId) {
        if (!card.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Карта не принадлежит текущему пользователю");
        }
    }

    private CardResponse toResponse(Card card) {
        String masked = cardMaskUtil.mask(encryptionStrategy.decrypt(card.getCardNumberEncrypted()));
        return cardMapper.toResponse(card, masked);
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        String fromMasked = cardMaskUtil.mask(encryptionStrategy.decrypt(t.getFromCard().getCardNumberEncrypted()));
        String toMasked   = cardMaskUtil.mask(encryptionStrategy.decrypt(t.getToCard().getCardNumberEncrypted()));
        return new TransactionResponse(
                t.getId(),
                t.getFromCard().getId(),
                fromMasked,
                t.getToCard().getId(),
                toMasked,
                t.getAmount(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}