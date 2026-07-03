package com.example.bankcards.service.transfer;

public abstract class TransferHandler {

    private TransferHandler next;

    public final TransferHandler setNext(TransferHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(TransferContext context) {
        doHandle(context);
        if (next != null) {
            next.handle(context);
        }
    }

    protected abstract void doHandle(TransferContext context);
}
