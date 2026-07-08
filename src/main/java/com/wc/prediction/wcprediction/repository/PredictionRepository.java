package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    @Query("SELECT u, COALESCE(SUM(p.points), 0) + COALESCE(MAX(tp.totalPoints), 0) as totalPoints " +
            "FROM WcUser u " +
            "LEFT JOIN Prediction p ON u.id = p.user.id " +
            "LEFT JOIN TournamentPrediction tp ON u.id = tp.user.id " +
            "WHERE u.location = ?1 " +
            "GROUP BY u.id " +
            "ORDER BY (COALESCE(SUM(p.points), 0) + COALESCE(MAX(tp.totalPoints), 0)) DESC")
    List<Object[]> getLeaderboardByLocation(String location);

    @Query("SELECT DISTINCT p.matchId FROM Prediction p")
    List<String> findDistinctMatchIds();

    Optional<Prediction> findByUserAndMatchId(WcUser user, String matchId);

    Optional<List<Prediction>> findAllByMatchId(String matchId);

    Optional<List<Prediction>> findAllByUser(WcUser user);

    Optional<List<Prediction>> findAllByMatchIdIn(List<String> matchIds);

    Optional<List<Prediction>> findAllByUserAndMatchIdIn(WcUser user, List<String> matchIds);
}
