package com.wc.prediction.wcprediction.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchResultDto {
    private String matchId;
    private String match;
    private String matchResult; // TEAM_A_WIN, TEAM_B_WIN, DRAW
    private Integer scoreTeamA;
    private Integer scoreTeamB;
    private String firstGoalscorer;
    private String winningGoalscorer;
    private String playerOfMatch;
}
