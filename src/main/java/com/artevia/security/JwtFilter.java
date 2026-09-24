package com.artevia.security;

import com.artevia.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        var username = resolveUsername(request, response);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request, username);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveUsername(HttpServletRequest request, HttpServletResponse response) {
        var accessToken = extractAccessToken(request);

        if (accessToken != null && jwtService.isTokenValid(accessToken)) {
            return jwtService.extractUsername(accessToken);
        }

        if (request.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
            return null;
        }

        return silentRefresh(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return CookieUtils.extractCookie(request, "accessToken");
    }

    private String silentRefresh(HttpServletRequest request, HttpServletResponse response) {
        var refreshTokenValue = CookieUtils.extractCookie(request, "refreshToken");
        if (refreshTokenValue == null) {
            return null;
        }

        RefreshTokenService.SilentRefreshResult result;
        try {
            result = refreshTokenService.silentRefresh(refreshTokenValue);
        } catch (ObjectOptimisticLockingFailureException ex) {
            logger.debug("Refresh silenzioso già eseguito da una richiesta concorrente");
            return null;
        }
        if (result == null) {
            return null;
        }

        CookieUtils.addAuthCookie(response, "accessToken", result.newAccessToken(),
                result.accessTokenExpirationMs() / 1000);
        CookieUtils.addAuthCookie(response, "refreshToken", result.newRefreshToken(),
                result.refreshTokenExpirationMs() / 1000);

        return result.username();
    }

    private void authenticate(HttpServletRequest request, String username) {
        var userDetails = userDetailsService.loadUserByUsername(username);
        var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}