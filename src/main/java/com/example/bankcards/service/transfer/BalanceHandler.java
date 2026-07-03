package com.example.bankcards.service.transfer;

import com.example.bankcards.exception.InsufficientFundsException;

public class BalanceHandler extends TransferHandler {

    @Override
    protected void doHandle(TransferContext ctx) {
        if (ctx.from().getBalance().compareTo(ctx.amount()) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на карте-источнике");
        }
    }
}