package com.artevia.securitytest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void lo_stesso_refresh_token_non_puo_essere_riutilizzato() throws Exception {
        Cookie[] cookieRegistrazione = registraEAccedi("rotationuser", "rotationuser@test.com");
        Cookie refreshTokenOriginale = trovaCookie(cookieRegistrazione, "refreshToken");

        var risultatoPrimoRefresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshTokenOriginale))
                .andExpect(status().isOk())
                .andReturn();

        Cookie nuovoRefreshToken = trovaCookie(risultatoPrimoRefresh.getResponse().getCookies(), "refreshToken");

        org.junit.jupiter.api.Assertions.assertNotEquals(
                refreshTokenOriginale.getValue(), nuovoRefreshToken.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshTokenOriginale))
                .andExpect(status().isBadRequest());
    }

    private Cookie trovaCookie(Cookie[] cookies, String nome) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(nome)) return cookie;
        }
        throw new IllegalStateException("Cookie " + nome + " non trovato nella risposta");
    }

    private Cookie[] registraEAccedi(String username, String email) throws Exception {
        String registerBody = """
                {"username":"%s","name":"Test","lastname":"User","email":"%s","address":"Via Test 1","age":25,"password":"Password1!"}
                """.formatted(username, email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isOk());

        String loginBody = """
                {"usernameOrEmail":"%s","password":"Password1!"}
                """.formatted(username);

        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }
    
    @Test
    void il_refresh_silenzioso_del_filtro_ruota_anch_esso_il_token() throws Exception {
        Cookie[] cookies = registraEAccedi("silentrotationuser", "silentrotationuser@test.com");
        Cookie refreshTokenOriginale = trovaCookie(cookies, "refreshToken");
        Cookie accessTokenFinto = new Cookie("accessToken", "token-scaduto-o-non-valido");

        // Simula un access token scaduto: il filtro deve attivare il refresh silenzioso
        var risultato = mockMvc.perform(get("/api/v1/wallet/mywallet")
                        .cookie(accessTokenFinto, refreshTokenOriginale))
                .andExpect(status().isOk())
                .andReturn();

        Cookie nuovoRefreshToken = trovaCookie(risultato.getResponse().getCookies(), "refreshToken");
        org.junit.jupiter.api.Assertions.assertNotEquals(
                refreshTokenOriginale.getValue(), nuovoRefreshToken.getValue(),
                "Anche il refresh silenzioso del filtro deve ruotare il refresh token");

        // Il refresh token "vecchio" non deve più essere utilizzabile
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshTokenOriginale))
                .andExpect(status().isBadRequest());
    }
}