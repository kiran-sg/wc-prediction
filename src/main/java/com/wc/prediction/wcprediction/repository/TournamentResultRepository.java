package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.TournamentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TournamentResultRepository extends JpaRepository<TournamentResult, Long> {
    Optional<TournamentResult> findFirstByOrderByIdAsc();
}
