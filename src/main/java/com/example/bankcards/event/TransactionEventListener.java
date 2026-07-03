package com.example.bankcards.event;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final TransactionRepository transactionRepository;

    @EventListener
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
