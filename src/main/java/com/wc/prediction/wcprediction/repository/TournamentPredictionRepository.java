package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.TournamentPrediction;
import com.wc.prediction.wcprediction.entity.WcUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TournamentPredictionRepository extends JpaRepository<TournamentPrediction, Long> {
    Optional<TournamentPrediction> findByUser(WcUser user);
}
