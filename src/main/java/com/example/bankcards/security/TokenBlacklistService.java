package com.example.bankcards.security;

import com.example.bankcards.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final CacheManager cacheManager;

    public void blacklist(String jti) {
        cache().put(jti, true);
    }

    public boolean isBlacklisted(String jti) {
        Cache.ValueWrapper value = cache().get(jti);
        return value != null;
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(CacheConfig.TOKEN_BLACKLIST_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Кеш " + CacheConfig.TOKEN_BLACKLIST_CACHE + " не зарегистрирован");
        }
        return cache;
    }
}
