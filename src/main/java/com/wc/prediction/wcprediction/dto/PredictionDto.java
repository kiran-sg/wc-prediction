package com.wc.prediction.wcprediction.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PredictionDto {

    private Long predictionId;
    private String matchId;
    private String match;
    private String matchDateTime;
    private String matchDate;
    private String userId;
    private WcUserDto user;

    private String matchResultPredicted;
    private Integer scoreTeamAPredicted;
    private Integer scoreTeamBPredicted;
    private String firstGoalscorerPredicted;
    private String winningGoalscorerPredicted;
    private String playerOfMatchPredicted;
    private LocalDateTime predictionTime;

    private Integer points;
}
