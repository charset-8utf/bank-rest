package com.example.bankcards.entity;

import com.example.bankcards.exception.CardNotActiveException;
import org.jspecify.annotations.Nullable;

public enum CardStatus {

    ACTIVE {
        @Override
        public void requireActive(@Nullable String role) {
            // Карта активна — операция разрешена
        }

        @Override
        public void requireActivatable() {
            // Карта уже активна — повторная активация допустима как no-op
        }
    },
    BLOCKED {
        @Override
        public void requireActive(@Nullable String role) {
            throw new CardNotActiveException(
                    role != null ? "Карта-" + role + " не активна: заблокирована"
                                 : "Карта не активна: заблокирована"
            );
        }

        @Override
        public void requireActivatable() {
            // Заблокированная карта может быть активирована администратором
        }
    },
    EXPIRED {
        @Override
        public void requireActive(@Nullable String role) {
            throw new CardNotActiveException(
                    role != null ? "Карта-" + role + " не активна: срок действия истёк"
                                 : "Карта не активна: срок действия истёк"
            );
        }

        @Override
        public void requireActivatable() {
            throw new CardNotActiveException("Нельзя активировать карту с истёкшим сроком действия");
        }
    };

    public final void requireActive() {
        requireActive(null);
    }

    public abstract void requireActive(@Nullable String role);

    public abstract void requireActivatable();
}