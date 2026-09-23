package com.artevia.securitytest;

import com.artevia.model.Role;
import com.artevia.repository.UserRepository;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class MassAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registrazione_ignora_il_campo_role_iniettato_dal_client() throws Exception {
        Map<String, Object> payloadConCampoExtra = Map.of(
                "username", "hacker_test",
                "name", "Mario",
                "lastname", "Rossi",
                "email", "hacker@test.com",
                "address", "Via Test 1",
                "age", 30,
                "password", "Password1!",
                "role", "ADMIN"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payloadConCampoExtra)))
                .andExpect(status().isOk());

        Role ruoloEffettivo = userRepository.findByUsername("hacker_test")
                .orElseThrow()
                .getRole();

        assertEquals(Role.USER, ruoloEffettivo, "Il ruolo deve sempre essere USER di default, mai quello inviato dal client");
    }
}