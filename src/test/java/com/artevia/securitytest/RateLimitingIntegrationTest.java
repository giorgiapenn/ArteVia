package com.artevia.securitytest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oltre_il_limite_di_richieste_al_minuto_risponde_429() throws Exception {
        Cookie[] cookies = registraEAccedi("ratelimituser", "ratelimituser@test.com");

        for (int numeroRichiesta = 1; numeroRichiesta <= 5; numeroRichiesta++) {
            int status = mockMvc.perform(get("/api/v1/artwork/featured").cookie(cookies))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            org.junit.jupiter.api.Assertions.assertNotEquals(429, status,
                    "La richiesta numero " + numeroRichiesta + " non doveva ancora essere limitata");
        }

        int statoSestaRichiesta = mockMvc.perform(get("/api/v1/artwork/featured").cookie(cookies))
                .andReturn()
                .getResponse()
                .getStatus();

        assertEquals(429, statoSestaRichiesta, "La sesta richiesta in un minuto deve essere bloccata dal rate limiter");
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
}