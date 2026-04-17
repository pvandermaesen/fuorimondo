package com.fuorimondo.legal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void getExistingSlugReturns200() throws Exception {
        mvc.perform(get("/api/legal/cgu").param("locale", "FR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("cgu"))
            .andExpect(jsonPath("$.markdown").exists());
    }

    @Test
    void unknownSlugReturns404() throws Exception {
        mvc.perform(get("/api/legal/unknown"))
            .andExpect(status().isNotFound());
    }

    @Test
    void italianLocaleReturnsContent() throws Exception {
        mvc.perform(get("/api/legal/cgv").param("locale", "IT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locale").value("IT"));
    }
}
