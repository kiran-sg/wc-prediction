package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.WcPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<WcPlayer, Long> {
    List<WcPlayer> findByTeamIn(List<String> teams);

    List<WcPlayer> findByTeam(String team);
}
