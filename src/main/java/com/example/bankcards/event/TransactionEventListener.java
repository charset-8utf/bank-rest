package com.example.bankcards.event;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final TransactionRepository transactionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferCompleted(TransferCompletedEvent event) {
        transactionRepository.save(Transaction.builder()
                .fromCard(event.from())
                .toCard(event.to())
                .amount(event.amount())
                .description("Перевод")
                .build());
        log.debug("Транзакция записана: fromCard={}, toCard={}",
                event.from().getId(), event.to().getId());
    }
}
