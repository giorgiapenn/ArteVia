package com.artevia.securitytest;

import com.artevia.model.Product;
import com.artevia.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class CheckoutTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void checkout_con_saldo_insufficiente_non_deve_modificare_lo_stock() throws Exception {
        Product prodottoCostoso = productRepository.save(Product.builder()
                .name("Tela dipinta a mano")
                .description("Opera originale")
                .price(new BigDecimal("100.00"))
                .stockQuantity(10)
                .category("Dipinti")
                .build());

        Cookie[] cookies = registraEAccedi("checkoutuser", "checkoutuser@test.com");

        String carrello = """
                {"items":[{"id": %d, "quantity": 2}]}
                """.formatted(prodottoCostoso.getId());

        mockMvc.perform(post("/api/v1/shop/checkout")
                        .cookie(cookies)
                        .contentType("application/json")
                        .content(carrello))
                .andExpect(status().isBadRequest());

        Product prodottoRicaricato = productRepository.findById(prodottoCostoso.getId()).orElseThrow();
        assertEquals(10, prodottoRicaricato.getStockQuantity(),
                "Lo stock non deve mai essere scalato se il pagamento fallisce");
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