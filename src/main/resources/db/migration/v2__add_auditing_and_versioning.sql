--liquibase formatted sql

--changeset bankcards:4-add-updated-at-users
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

--changeset bankcards:5-add-updated-at-cards
ALTER TABLE cards ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

--changeset bankcards:6-add-version-cards
ALTER TABLE cards ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
