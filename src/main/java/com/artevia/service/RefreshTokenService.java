package com.artevia.service;

import com.artevia.model.RefreshToken;
import com.artevia.model.User;
import com.artevia.repository.RefreshTokenRepository;
import com.artevia.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token.expiration-ms}")
    private final long refreshTokenExpirationMs;

    @Transactional
    public SilentRefreshResult silentRefresh(String refreshTokenValue) {
        if (refreshTokenValue == null) return null;

        var rtOpt = refreshTokenRepository.findByToken(refreshTokenValue);
        if (rtOpt.isEmpty() || rtOpt.get().getExpiryDate().isBefore(Instant.now())) {
            return null;
        }

        var rt = rtOpt.get();
        var user = rt.getUser();
        refreshTokenRepository.delete(rt);

        var newRefreshToken = issueRefreshToken(user);
        var newAccessToken = jwtService.generateAccessToken(user.getUsername());

        return new SilentRefreshResult(
                user.getUsername(),
                newAccessToken,
                newRefreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(),
                refreshTokenExpirationMs
        );
    }

    private RefreshToken issueRefreshToken(User user) {
        var refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public record SilentRefreshResult(
            String username,
            String newAccessToken,
            String newRefreshToken,
            long accessTokenExpirationMs,
            long refreshTokenExpirationMs
    ) {}
}