--liquibase formatted sql

--changeset bankcards:7-create-roles-table
CREATE TABLE roles
(
    id         BIGSERIAL   NOT NULL,
    name       VARCHAR(16) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

--changeset bankcards:8-seed-roles
INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');

--changeset bankcards:9-create-user-roles-table
CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

--changeset bankcards:10-migrate-role-column
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = u.role;

--changeset bankcards:11-drop-role-column
ALTER TABLE users DROP COLUMN role;

--changeset bankcards:12-create-user-profiles-table
CREATE TABLE user_profiles
(
    id         BIGSERIAL    NOT NULL,
    user_id    BIGINT       NOT NULL,
    first_name VARCHAR(64),
    last_name  VARCHAR(64),
    phone      VARCHAR(20),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

--changeset bankcards:13-create-transactions-table
CREATE TABLE transactions
(
    id           BIGSERIAL      NOT NULL,
    from_card_id BIGINT         NOT NULL,
    to_card_id   BIGINT         NOT NULL,
    amount       DECIMAL(19, 2) NOT NULL,
    description  VARCHAR(255),
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_from_card FOREIGN KEY (from_card_id) REFERENCES cards (id),
    CONSTRAINT fk_transactions_to_card FOREIGN KEY (to_card_id) REFERENCES cards (id)
);

--changeset bankcards:14-create-transaction-indexes
CREATE INDEX idx_transactions_from_card_id ON transactions (from_card_id);
CREATE INDEX idx_transactions_to_card_id ON transactions (to_card_id);
