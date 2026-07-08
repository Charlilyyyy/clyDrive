package com.clydrive.service.impl;

import com.clydrive.module.TokenBlacklist;
import com.clydrive.repository.TokenBlacklistRepository;
import com.clydrive.service.TokenBlacklistService;
import com.clydrive.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtUtil jwtUtil;

    public TokenBlacklistServiceImpl(TokenBlacklistRepository tokenBlacklistRepository, JwtUtil jwtUtil) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public void blacklistToken(String token) {
        String jti = jwtUtil.extractJti(token);

        if (tokenBlacklistRepository.existsByJti(jti)) {
            return;
        }

        TokenBlacklist blacklisted = TokenBlacklist.builder()
                .jti(jti)
                .expiryDate(jwtUtil.extractExpiration(token))
                .build();

        tokenBlacklistRepository.save(blacklisted);
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            return tokenBlacklistRepository.existsByJti(jwtUtil.extractJti(token));
        } catch (Exception e) {
            return true;
        }
    }
}
