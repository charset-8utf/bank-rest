package com.example.bankcards.config;

import com.example.bankcards.service.transfer.BalanceHandler;
import com.example.bankcards.service.transfer.CardStatusHandler;
import com.example.bankcards.service.transfer.OwnershipHandler;
import com.example.bankcards.service.transfer.TransferHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransferValidationConfig {

    @Bean
    public TransferHandler transferValidationChain() {
        OwnershipHandler ownership = new OwnershipHandler();
        ownership.setNext(new CardStatusHandler()).setNext(new BalanceHandler());
        return ownership;
    }
}