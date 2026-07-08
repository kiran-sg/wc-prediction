package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wc_tournament_result")
public class TournamentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}
