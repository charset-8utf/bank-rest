ALTER TABLE user_profiles
    DROP CONSTRAINT fk_user_profiles_user,
    ADD CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
