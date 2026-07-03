package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardMaskUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardEncryptionUtil encryptionUtil;
    @Mock private CardMaskUtil cardMaskUtil;
    @Mock private CardMapper cardMapper;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private User owner;
    private User otherUser;
    private Card activeCard;
    private Card secondCard;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().name(RoleType.USER).build();
        owner = User.builder().id(1L).username("alice").roles(new java.util.HashSet<>(java.util.List.of(userRole))).build();
        otherUser = User.builder().id(2L).username("bob").roles(new java.util.HashSet<>(java.util.List.of(userRole))).build();

        activeCard = Card.builder()
                .id(10L)
                .cardNumberEncrypted("encrypted-1")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .createdAt(LocalDateTime.now())
                .build();

        secondCard = Card.builder()
                .id(20L)
                .cardNumberEncrypted("encrypted-2")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void stubCardMapping() {
        when(encryptionUtil.decrypt(any())).thenReturn("1234567890123456");
        when(cardMaskUtil.mask(any())).thenReturn("**** **** **** 3456");
        when(cardMapper.toResponse(any(Card.class), any(String.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            String masked = inv.getArgument(1);
            return new CardResponse(c.getId(), masked,
                    c.getOwner() != null ? c.getOwner().getUsername() : null,
                    c.getExpiryDate(), c.getStatus(), c.getBalance(), c.getCreatedAt());
        });
    }

    @Test
    void createCard_success_returnsResponse() {
        stubCardMapping();
        CardCreateRequest request = new CardCreateRequest(1L, "1234567890123456", LocalDate.now().plusYears(3));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(encryptionUtil.encrypt("1234567890123456")).thenReturn("encrypted");
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c = Card.builder()
                    .id(10L).cardNumberEncrypted("encrypted").owner(owner)
                    .expiryDate(c.getExpiryDate()).status(c.getStatus())
                    .balance(BigDecimal.ZERO).createdAt(LocalDateTime.now()).build();
            return c;
        });

        CardResponse response = cardService.createCard(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.maskedCardNumber()).isEqualTo("**** **** **** 3456");
    }

    @Test
    void createCard_ownerNotFound_throwsException() {
        CardCreateRequest request = new CardCreateRequest(99L, "1234567890123456", LocalDate.now().plusYears(1));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blockCard_success_changesStatus() {
        stubCardMapping();
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(activeCard)).thenReturn(activeCard);

        CardResponse response = cardService.blockCard(10L);

        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void activateCard_success_changesStatus() {
        stubCardMapping();
        activeCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(activeCard)).thenReturn(activeCard);

        CardResponse response = cardService.activateCard(10L);

        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void activateCard_expiredCard_throwsException() {
        activeCard.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.activateCard(10L))
                .isInstanceOf(CardNotActiveException.class);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void transfer_success_updatesBalances() {
        TransferRequest request = new TransferRequest(10L, 20L, BigDecimal.valueOf(300));
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(secondCard));
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cardService.transfer(request, 1L);

        assertThat(activeCard.getBalance()).isEqualByComparingTo("700");
        assertThat(secondCard.getBalance()).isEqualByComparingTo("800");
        verify(cardRepository, times(2)).save(any());
    }

    @Test
    void transfer_sameCard_throwsIllegalArgument() {
        TransferRequest request = new TransferRequest(10L, 10L, BigDecimal.valueOf(100));

        assertThatThrownBy(() -> cardService.transfer(request, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_insufficientFunds_throwsException() {
        TransferRequest request = new TransferRequest(10L, 20L, BigDecimal.valueOf(9999));
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(secondCard));

        assertThatThrownBy(() -> cardService.transfer(request, 1L))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_sourceCardBlocked_throwsException() {
        activeCard.setStatus(CardStatus.BLOCKED);
        TransferRequest request = new TransferRequest(10L, 20L, BigDecimal.valueOf(100));
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(secondCard));

        assertThatThrownBy(() -> cardService.transfer(request, 1L))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("источник");
    }

    @Test
    void transfer_cardBelongsToOtherUser_throwsAccessDenied() {
        activeCard.setOwner(otherUser);
        TransferRequest request = new TransferRequest(10L, 20L, BigDecimal.valueOf(100));
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(secondCard));

        assertThatThrownBy(() -> cardService.transfer(request, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requestBlock_success_blocksCard() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(activeCard)).thenReturn(activeCard);

        cardService.requestBlock(10L, 1L);

        assertThat(activeCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void requestBlock_cardAlreadyBlocked_throwsException() {
        activeCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.requestBlock(10L, 1L))
                .isInstanceOf(CardNotActiveException.class);
    }

    @Test
    void requestBlock_notOwner_throwsAccessDenied() {
        activeCard.setOwner(otherUser);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.requestBlock(10L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteCard_found_deletesCard() {
        when(cardRepository.existsById(10L)).thenReturn(true);

        cardService.deleteCard(10L);

        verify(cardRepository).deleteById(10L);
    }

    @Test
    void deleteCard_notFound_throwsException() {
        when(cardRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> cardService.deleteCard(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllCards_withoutStatus_returnsAll() {
        stubCardMapping();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(activeCard));
        when(cardRepository.findAll(pageable)).thenReturn(page);

        PageResponse<CardResponse> response = cardService.getAllCards(null, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void getAllCards_withStatus_filtersCards() {
        stubCardMapping();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(activeCard));
        when(cardRepository.findByStatus(CardStatus.ACTIVE, pageable)).thenReturn(page);

        PageResponse<CardResponse> response = cardService.getAllCards(CardStatus.ACTIVE, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        verify(cardRepository).findByStatus(CardStatus.ACTIVE, pageable);
    }

    @Test
    void getMyCards_withoutStatus_returnsOwnerCards() {
        stubCardMapping();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(activeCard));
        when(cardRepository.findByOwnerId(1L, pageable)).thenReturn(page);

        PageResponse<CardResponse> response = cardService.getMyCards(1L, null, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        verify(cardRepository).findByOwnerId(1L, pageable);
    }

    @Test
    void getMyCards_withStatus_filtersOwnerCards() {
        stubCardMapping();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(activeCard));
        when(cardRepository.findByOwnerIdAndStatus(1L, CardStatus.ACTIVE, pageable)).thenReturn(page);

        PageResponse<CardResponse> response = cardService.getMyCards(1L, CardStatus.ACTIVE, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        verify(cardRepository).findByOwnerIdAndStatus(1L, CardStatus.ACTIVE, pageable);
    }

    @Test
    void transfer_destinationCardBlocked_throwsException() {
        secondCard.setStatus(CardStatus.BLOCKED);
        TransferRequest request = new TransferRequest(10L, 20L, BigDecimal.valueOf(100));
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(secondCard));

        assertThatThrownBy(() -> cardService.transfer(request, 1L))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("получатель");
    }

    @Test
    void getCardTransactions_ownCard_returnsPaginatedTransactions() {
        when(encryptionUtil.decrypt(any())).thenReturn("1234567890123456");
        when(cardMaskUtil.mask(any())).thenReturn("**** **** **** 3456");
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx = Transaction.builder()
                .fromCard(activeCard)
                .toCard(secondCard)
                .amount(BigDecimal.valueOf(100))
                .description("Перевод")
                .build();
        Page<Transaction> txPage = new PageImpl<>(List.of(tx), pageable, 1);

        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(transactionRepository.findByCardId(10L, pageable)).thenReturn(txPage);

        PageResponse<TransactionResponse> response = cardService.getCardTransactions(10L, 1L, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().amount()).isEqualByComparingTo("100");
    }

    @Test
    void getCardTransactions_notOwner_throwsAccessDenied() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.getCardTransactions(10L, 2L, PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(transactionRepository, never()).findByCardId(any(), any());
    }
}