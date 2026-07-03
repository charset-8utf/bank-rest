package com.example.bankcards.service.transfer;

public class CardStatusHandler extends TransferHandler {

    @Override
    protected void doHandle(TransferContext ctx) {
        ctx.from().getStatus().requireActive("источник");
        ctx.to().getStatus().requireActive("получатель");
    }
}
