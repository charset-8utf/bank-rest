package com.example.bankcards.service;

import com.example.bankcards.entity.User;

public interface RefreshTokenService {

    String create(User user);

    User validate(String token);

    void delete(String token);
}
