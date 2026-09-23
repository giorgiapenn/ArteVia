package com.artevia.controller;

import com.artevia.dto.LoginRequest;
import com.artevia.dto.RegisterRequest;
import com.artevia.dto.TokenResponse;
import com.artevia.security.CookieUtils;
import com.artevia.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Map.of("message", "Utente registrato con successo");
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        var tokens = authService.login(req);
        setAuthCookies(response, tokens);
        return tokens;
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @RequestBody(required = false) Map<String, String> body) {

        var refreshTokenValue = CookieUtils.extractCookie(request, "refreshToken");
        if (refreshTokenValue == null && body != null) {
            refreshTokenValue = body.get("refreshToken");
        }

        var tokens = authService.refresh(refreshTokenValue);
        setAuthCookies(response, tokens);
        return tokens;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response,
                                      @RequestBody(required = false) Map<String, String> body) {
        var refreshTokenValue = CookieUtils.extractCookie(request, "refreshToken");
        if (refreshTokenValue == null && body != null) {
            refreshTokenValue = body.get("refreshToken");
        }
        authService.logout(refreshTokenValue);
        clearAuthCookies(response);
        return Map.of("message", "Logout effettuato");
    }

    private void setAuthCookies(HttpServletResponse response, TokenResponse tokens) {
        CookieUtils.addAuthCookie(response, "accessToken", tokens.accessToken(), tokens.expiresIn());
        CookieUtils.addAuthCookie(response, "refreshToken", tokens.refreshToken(), tokens.refreshExpiresIn());
    }
     
    private void clearAuthCookies(HttpServletResponse response) {
        CookieUtils.addAuthCookie(response, "accessToken", "", 0);
        CookieUtils.addAuthCookie(response, "refreshToken", "", 0);
    }
}