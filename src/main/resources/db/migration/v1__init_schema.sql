--liquibase formatted sql

--changeset bankcards:1-create-users-table
CREATE TABLE users
(
    id         BIGSERIAL    NOT NULL,
    username   VARCHAR(64)  NOT NULL,
    email      VARCHAR(128) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(16)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

--changeset bankcards:2-create-cards-table
CREATE TABLE cards
(
    id                    BIGSERIAL      NOT NULL,
    card_number_encrypted VARCHAR(512)   NOT NULL,
    owner_id              BIGINT         NOT NULL,
    expiry_date           DATE           NOT NULL,
    status                VARCHAR(16)    NOT NULL,
    balance               DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT fk_cards_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

--changeset bankcards:3-create-indexes
CREATE INDEX idx_cards_owner_id ON cards (owner_id);
CREATE INDEX idx_cards_status ON cards (status);