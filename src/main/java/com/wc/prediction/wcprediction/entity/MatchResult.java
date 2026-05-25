package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wc_match_results")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true)
    private String matchId;

    @Column(name = "match_result", nullable = false)
    private String matchResult; // TEAM_A_WIN, TEAM_B_WIN, DRAW

    @Column(name = "score_team_a", nullable = false)
    private Integer scoreTeamA;

    @Column(name = "score_team_b", nullable = false)
    private Integer scoreTeamB;

    @Column(name = "first_goalscorer", nullable = false)
    private String firstGoalscorer;

    @Column(name = "winning_goalscorer", nullable = false)
    private String winningGoalscorer;

    @Column(name = "player_of_match", nullable = false)
    private String playerOfMatch;

    @Column(name = "result_updated_time")
    private LocalDateTime resultUpdatedTime;
}
