package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.WcMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<WcMatch, Long> {
    Optional<WcMatch> findByMatchNo(String matchNo);

    @Query("SELECT m FROM WcMatch m ORDER BY CASE WHEN m.dateTime IS NULL OR m.dateTime = '' THEN 1 ELSE 0 END, m.dateTime")
    List<WcMatch> findAllOrderByDateTime();
}
