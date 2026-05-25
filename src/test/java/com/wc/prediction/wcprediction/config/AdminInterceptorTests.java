package com.wc.prediction.wcprediction.config;

import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminInterceptorTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        WcUser admin = new WcUser();
        admin.setUserId("admin1");
        admin.setName("Admin");
        admin.setIsAdmin(true);
        userRepository.save(admin);

        WcUser regular = new WcUser();
        regular.setUserId("user1");
        regular.setName("Regular User");
        regular.setIsAdmin(false);
        userRepository.save(regular);
    }

    @Test
    void adminApi_noSession_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/predictions/match").param("matchId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    @Test
    void adminApi_regularUser_returns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", "user1");

        mockMvc.perform(get("/api/admin/predictions/match").param("matchId", "1").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    @Test
    void adminApi_adminUser_returns200() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", "admin1");

        mockMvc.perform(get("/api/admin/predictions/match").param("matchId", "1").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void adminApi_invalidUserId_returns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", "nonexistent");

        mockMvc.perform(get("/api/admin/predictions/match").param("matchId", "1").session(session))
                .andExpect(status().isForbidden());
    }
}
