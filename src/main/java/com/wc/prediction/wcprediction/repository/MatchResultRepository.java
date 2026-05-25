package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    Optional<MatchResult> findByMatchId(String matchId);
}
