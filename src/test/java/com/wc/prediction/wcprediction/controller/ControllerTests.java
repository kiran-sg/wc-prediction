package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.entity.WcPlayer;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();
        matchRepository.deleteAll();

        WcPlayer p1 = new WcPlayer();
        p1.setPlayerName("Vinicius Jr");
        p1.setPosition("FWD");
        p1.setTeam("Brazil");

        WcPlayer p2 = new WcPlayer();
        p2.setPlayerName("Lionel Messi");
        p2.setPosition("FWD");
        p2.setTeam("Argentina");

        playerRepository.saveAll(List.of(p1, p2));

        WcMatch m1 = new WcMatch();
        m1.setMatchNo("1");
        m1.setDateTime("2026-06-11T21:00:00");
        m1.setTeamA("Brazil");
        m1.setTeamB("Argentina");

        matchRepository.save(m1);
    }

    @Test
    void getAllMatches_returnsMatches() throws Exception {
        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].matchNo").value("1"))
                .andExpect(jsonPath("$[0].teamA").value("Brazil"));
    }

    @Test
    void getPlayersByTeam_withFilter() throws Exception {
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teams\":[\"Brazil\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].playerName").value("Vinicius Jr"));
    }

    @Test
    void getPlayersByTeam_noFilter_returnsAll() throws Exception {
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teams\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
