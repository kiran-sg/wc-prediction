package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.entity.WcPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTests {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();
        matchRepository.deleteAll();

        WcPlayer player1 = new WcPlayer();
        player1.setPlayerName("Vinicius Jr");
        player1.setPosition("FWD");
        player1.setTeam("Brazil");

        WcPlayer player2 = new WcPlayer();
        player2.setPlayerName("Lionel Messi");
        player2.setPosition("FWD");
        player2.setTeam("Argentina");

        WcPlayer player3 = new WcPlayer();
        player3.setPlayerName("Enzo Fernandez");
        player3.setPosition("MID");
        player3.setTeam("Argentina");

        playerRepository.saveAll(List.of(player1, player2, player3));

        WcMatch match1 = new WcMatch();
        match1.setMatchNo("1");
        match1.setDateTime("2026-06-11T21:00:00");
        match1.setTeamA("Brazil");
        match1.setTeamB("Argentina");

        WcMatch match2 = new WcMatch();
        match2.setMatchNo("2");
        match2.setDateTime("2026-06-12T18:00:00");
        match2.setTeamA("Mexico");
        match2.setTeamB("Indonesia");

        matchRepository.saveAll(List.of(match1, match2));
    }

    @Test
    void findAllPlayers_returnsAll() {
        List<WcPlayer> players = playerRepository.findAll();
        assertThat(players).hasSize(3);
    }

    @Test
    void findPlayersByTeam_filtersCorrectly() {
        List<WcPlayer> argPlayers = playerRepository.findByTeamIn(List.of("Argentina"));
        assertThat(argPlayers).hasSize(2);
        assertThat(argPlayers).allMatch(p -> p.getTeam().equals("Argentina"));
    }

    @Test
    void findPlayersByMultipleTeams() {
        List<WcPlayer> players = playerRepository.findByTeamIn(List.of("Argentina", "Brazil"));
        assertThat(players).hasSize(3);
    }

    @Test
    void findPlayersByTeam_noMatch_returnsEmpty() {
        List<WcPlayer> players = playerRepository.findByTeamIn(List.of("Japan"));
        assertThat(players).isEmpty();
    }

    @Test
    void findAllMatches_returnsAll() {
        List<WcMatch> matches = matchRepository.findAll();
        assertThat(matches).hasSize(2);
    }

    @Test
    void findMatchByMatchNo_found() {
        Optional<WcMatch> match = matchRepository.findByMatchNo("1");
        assertThat(match).isPresent();
        assertThat(match.get().getTeamA()).isEqualTo("Brazil");
        assertThat(match.get().getTeamB()).isEqualTo("Argentina");
    }

    @Test
    void findMatchByMatchNo_notFound() {
        Optional<WcMatch> match = matchRepository.findByMatchNo("99");
        assertThat(match).isEmpty();
    }
}
