package com.artevia.securitytest;

import com.artevia.model.Role;
import com.artevia.model.User;
import com.artevia.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private static final String NUOVO_PRODOTTO_JSON = """
            {"name":"Quadro di prova","description":"desc","price":10.00,"stockQuantity":5,"category":"Dipinti"}
            """;

    @Test
    void un_utente_normale_non_puo_creare_prodotti() throws Exception {
        Cookie[] cookies = registraEAccedi("userplain", "userplain@test.com");

        mockMvc.perform(post("/api/v1/admin/products")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content(NUOVO_PRODOTTO_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void un_admin_puo_creare_prodotti() throws Exception {
        Cookie[] cookies = registraEAccedi("adminplain", "adminplain@test.com");

        User utente = userRepository.findByUsername("adminplain").orElseThrow();
        utente.setRole(Role.ADMIN);
        userRepository.save(utente);

        mockMvc.perform(post("/api/v1/admin/products")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content(NUOVO_PRODOTTO_JSON))
                .andExpect(status().isCreated());
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