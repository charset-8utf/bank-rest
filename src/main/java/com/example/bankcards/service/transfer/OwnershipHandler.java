package com.example.bankcards.service.transfer;

import org.springframework.security.access.AccessDeniedException;

public class OwnershipHandler extends TransferHandler {

    @Override
    protected void doHandle(TransferContext ctx) {
        if (!ctx.from().getOwner().getId().equals(ctx.userId())) {
            throw new AccessDeniedException("Карта не принадлежит текущему пользователю");
        }
        if (!ctx.to().getOwner().getId().equals(ctx.userId())) {
            throw new AccessDeniedException("Карта не принадлежит текущему пользователю");
        }
    }
}