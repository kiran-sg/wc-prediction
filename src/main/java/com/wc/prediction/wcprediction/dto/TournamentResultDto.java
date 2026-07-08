package com.wc.prediction.wcprediction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TournamentResultDto {
    @NotBlank private String goldenBall;
    @NotBlank private String goldenBoot;
    @NotBlank private String goldenGlove;
    @NotBlank private String youngPlayer;
    @NotBlank private String fairPlayTeam;
}
