package com.wc.prediction.wcprediction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TournamentPredictionDto {
    private Long id;
    @NotBlank private String userId;
    private String userName;
    @NotBlank private String goldenBall;
    @NotBlank private String goldenBoot;
    @NotBlank private String goldenGlove;
    @NotBlank private String youngPlayer;
    @NotBlank private String fairPlayTeam;
    private Integer goldenBallPoints;
    private Integer goldenBootPoints;
    private Integer goldenGlovePoints;
    private Integer youngPlayerPoints;
    private Integer fairPlayPoints;
    private Integer totalPoints;
}
