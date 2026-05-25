package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wc_predictions")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long predictionId;

    @Column(name = "match_id", nullable = false)
    private String matchId;

    @ManyToOne
    @JoinColumn(name = "wc_user_id", nullable = false)
    private WcUser user;

    @Column(name = "match_result_predicted", nullable = false)
    private String matchResultPredicted;

    @Column(name = "score_team_a_predicted", nullable = false)
    private Integer scoreTeamAPredicted;

    @Column(name = "score_team_b_predicted", nullable = false)
    private Integer scoreTeamBPredicted;

    @Column(name = "first_goalscorer_predicted", nullable = false)
    private String firstGoalscorerPredicted;

    @Column(name = "winning_goalscorer_predicted", nullable = false)
    private String winningGoalscorerPredicted;

    @Column(name = "player_of_match_predicted", nullable = false)
    private String playerOfMatchPredicted;

    @Column(name = "prediction_time", nullable = false)
    private LocalDateTime predictionTime;

    @Column(name = "points")
    private Integer points;
}
