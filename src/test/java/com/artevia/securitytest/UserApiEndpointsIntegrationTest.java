package com.artevia.securitytest;

import com.artevia.model.ClubPlan;
import com.artevia.model.Product;
import com.artevia.repository.ClubPlanRepository;
import com.artevia.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class UserApiEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ClubPlanRepository clubPlanRepository;

    @Test
    void un_utente_vede_il_proprio_wallet_e_puo_ricaricarlo() throws Exception {
        Cookie[] cookies = registraEAccedi("walletuser", "walletuser@test.com");

        mockMvc.perform(get("/api/v1/wallet/mywallet").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").exists());

        mockMvc.perform(post("/api/v1/wallet/recharge")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content("{\"amount\": 50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    void un_utente_autenticato_vede_lelenco_prodotti_e_piani() throws Exception {
        productRepository.save(Product.builder()
                .name("Acquerello di prova")
                .description("Opera su carta")
                .price(new BigDecimal("15.00"))
                .stockQuantity(5)
                .category("Acquerelli")
                .build());

        clubPlanRepository.save(ClubPlan.builder()
                .name("Piano Base")
                .price(new BigDecimal("9.90"))
                .durationDays(30)
                .discountPercentage(10)
                .build());

        Cookie[] cookies = registraEAccedi("browseuser", "browseuser@test.com");

        mockMvc.perform(get("/api/v1/products").cookie(cookies))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/plans").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void un_utente_puo_acquistare_un_piano_membership() throws Exception {
        ClubPlan piano = clubPlanRepository.save(ClubPlan.builder()
                .name("Piano Premium")
                .price(new BigDecimal("9.90"))
                .durationDays(30)
                .discountPercentage(15)
                .build());

        Cookie[] cookies = registraEAccedi("membershipuser", "membershipuser@test.com");

        mockMvc.perform(post("/api/v1/wallet/recharge")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content("{\"amount\": 20.00}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/membership/buy")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content("{\"planId\": %d}".formatted(piano.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void un_acquisto_completato_compare_nello_storico() throws Exception {
        Product prodotto = productRepository.save(Product.builder()
                .name("Stampa numerata")
                .description("Edizione limitata")
                .price(new BigDecimal("15.00"))
                .stockQuantity(5)
                .category("Stampe")
                .build());

        Cookie[] cookies = registraEAccedi("historyuser", "historyuser@test.com");

        mockMvc.perform(post("/api/v1/wallet/recharge")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content("{\"amount\": 20.00}"))
                .andExpect(status().isOk());

        String carrello = """
                {"items":[{"id": %d, "quantity": 1}]}
                """.formatted(prodotto.getId());

        mockMvc.perform(post("/api/v1/shop/checkout")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content(carrello))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user/shop/history").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Stampa numerata"));
    }

    @Test
    void il_logout_invalida_il_refresh_token() throws Exception {
        Cookie[] cookies = registraEAccedi("logoutuser", "logoutuser@test.com");
        Cookie refreshToken = trovaCookie(cookies, "refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshToken))
                .andExpect(status().isOk());

        // Il refresh token appena invalidato non deve più poter essere usato
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshToken))
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
}