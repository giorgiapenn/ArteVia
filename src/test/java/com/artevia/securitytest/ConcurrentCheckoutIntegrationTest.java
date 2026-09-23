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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentCheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void due_checkout_concorrenti_sullo_stesso_ultimo_pezzo_non_devono_venderlo_due_volte() throws Exception {
        Product ultimoPezzo = productRepository.save(Product.builder()
                .name("Pezzo unico")
                .description("Solo 1 disponibile")
                .price(new BigDecimal("5.00"))
                .stockQuantity(1)
                .category("Test")
                .build());

        Cookie[] cookieA = registraEAccediERicarica("concA", "concA@test.com");
        Cookie[] cookieB = registraEAccediERicarica("concB", "concB@test.com");

        String carrello = """
                {"items":[{"id": %d, "quantity": 1}]}
                """.formatted(ultimoPezzo.getId());

        AtomicInteger successi = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        pool.submit(() -> eseguiCheckout(cookieA, carrello, successi, latch));
        pool.submit(() -> eseguiCheckout(cookieB, carrello, successi, latch));

        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, successi.get(), "Solo UNO dei due checkout concorrenti deve riuscire, non entrambi");

        Product ricontrollato = productRepository.findById(ultimoPezzo.getId()).orElseThrow();
        assertEquals(0, ricontrollato.getStockQuantity(), "Lo stock finale deve essere 0, mai negativo");
    }

    private void eseguiCheckout(Cookie[] cookies, String carrello, AtomicInteger successi, CountDownLatch latch) {
        try {
            int status = mockMvc.perform(post("/api/v1/shop/checkout")
                            .cookie(cookies)
                            .contentType("application/json")
                            .content(carrello))
                    .andReturn().getResponse().getStatus();
            if (status == 200) successi.incrementAndGet();
        } catch (Exception ignored) {
        } finally {
            latch.countDown();
        }
    }

    private Cookie[] registraEAccediERicarica(String username, String email) throws Exception {
        String registerBody = """
                {"username":"%s","name":"Test","lastname":"User","email":"%s","address":"Via Test 1","age":25,"password":"Password1!"}
                """.formatted(username, email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(registerBody));

        String loginBody = """
                {"usernameOrEmail":"%s","password":"Password1!"}
                """.formatted(username);

        Cookie[] cookies = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andReturn().getResponse().getCookies();

        mockMvc.perform(post("/api/v1/wallet/recharge")
                .cookie(cookies)
                .contentType("application/json")
                .content("{\"amount\": 50.00}"));

        return cookies;
    }
}