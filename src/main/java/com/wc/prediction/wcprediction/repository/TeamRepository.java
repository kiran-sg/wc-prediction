package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.WcTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<WcTeam, Long> {
    Optional<WcTeam> findByShortName(String shortName);
    Optional<WcTeam> findByTeamName(String teamName);
}
