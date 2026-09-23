package com.artevia.service;

import com.artevia.dto.LoginRequest;
import com.artevia.dto.RegisterRequest;
import com.artevia.dto.TokenResponse;
import com.artevia.model.*;
import com.artevia.repository.*;
import com.artevia.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token.expiration-ms}")
    private final long refreshTokenExpirationMs;

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new IllegalArgumentException("Username già in uso");
        if (userRepository.existsByEmail(req.email()))
            throw new IllegalArgumentException("Email già in uso");

        var user = User.builder()
                .username(req.username())
                .name(req.name())
                .lastname(req.lastname())
                .email(req.email())
                .address(req.address())
                .age(req.age())
                .password(passwordEncoder.encode(req.password()))
                .build();

        userRepository.save(user);

        var wallet = Wallet.builder().user(user).balance(BigDecimal.ZERO).build();
        walletRepository.save(wallet);
    }

    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null) {
            refreshTokenRepository.findByToken(refreshTokenValue)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    public TokenResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.usernameOrEmail(), req.password()));

        var user = userRepository.findByUsername(req.usernameOrEmail())
                .or(() -> userRepository.findByEmail(req.usernameOrEmail()))
                .orElseThrow();

        var accessToken = jwtService.generateAccessToken(user.getUsername());
        var refreshToken = issueRefreshToken(user);

        return new TokenResponse(accessToken, refreshToken.getToken(), "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000, refreshTokenExpirationMs / 1000);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        var rt = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token non valido"));
        if (rt.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(rt);
            throw new IllegalArgumentException("Refresh token scaduto");
        }
        refreshTokenRepository.delete(rt);

        var newRefreshToken = issueRefreshToken(rt.getUser());
        var newAccessToken = jwtService.generateAccessToken(rt.getUser().getUsername());

        return new TokenResponse(newAccessToken, newRefreshToken.getToken(), "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000, refreshTokenExpirationMs / 1000);
    }

    private RefreshToken issueRefreshToken(User user) {
        var refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }
}