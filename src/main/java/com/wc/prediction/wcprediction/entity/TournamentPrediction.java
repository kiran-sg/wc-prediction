package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wc_tournament_predictions",
       uniqueConstraints = @UniqueConstraint(columnNames = "wc_user_id"))
public class TournamentPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wc_user_id", nullable = false)
    private WcUser user;

    @Column(name = "golden_ball")
    private String goldenBall;

    @Column(name = "golden_boot")
    private String goldenBoot;

    @Column(name = "golden_glove")
    private String goldenGlove;

    @Column(name = "young_player")
    private String youngPlayer;

    @Column(name = "fair_play_team")
    private String fairPlayTeam;

    @Column(name = "golden_ball_points")
    private Integer goldenBallPoints = 0;

    @Column(name = "golden_boot_points")
    private Integer goldenBootPoints = 0;

    @Column(name = "golden_glove_points")
    private Integer goldenGlovePoints = 0;

    @Column(name = "young_player_points")
    private Integer youngPlayerPoints = 0;

    @Column(name = "fair_play_points")
    private Integer fairPlayPoints = 0;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "prediction_time")
    private LocalDateTime predictionTime;
}
