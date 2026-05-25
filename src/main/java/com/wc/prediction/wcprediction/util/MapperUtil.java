package com.wc.prediction.wcprediction.util;

import com.wc.prediction.wcprediction.dto.PredictionDto;
import com.wc.prediction.wcprediction.dto.WcUserDto;
import com.wc.prediction.wcprediction.entity.Prediction;
import com.wc.prediction.wcprediction.entity.WcUser;

import java.sql.Timestamp;

public class MapperUtil {

    public static PredictionDto predictionToPredictionDto(Prediction prediction) {
        PredictionDto dto = new PredictionDto();
        dto.setPredictionId(prediction.getPredictionId());
        dto.setMatchId(prediction.getMatchId());
        dto.setUserId(prediction.getUser().getUserId());
        dto.setUser(wcUserToDto(prediction.getUser()));
        dto.setMatchResultPredicted(prediction.getMatchResultPredicted());
        dto.setScoreTeamAPredicted(prediction.getScoreTeamAPredicted());
        dto.setScoreTeamBPredicted(prediction.getScoreTeamBPredicted());
        dto.setFirstGoalscorerPredicted(prediction.getFirstGoalscorerPredicted());
        dto.setWinningGoalscorerPredicted(prediction.getWinningGoalscorerPredicted());
        dto.setPlayerOfMatchPredicted(prediction.getPlayerOfMatchPredicted());
        dto.setPredictionTime(prediction.getPredictionTime());
        dto.setPoints(prediction.getPoints() == null ? 0 : prediction.getPoints());
        return dto;
    }

    public static void updatePrediction(Prediction prediction, PredictionDto dto) {
        prediction.setMatchResultPredicted(dto.getMatchResultPredicted());
        prediction.setScoreTeamAPredicted(dto.getScoreTeamAPredicted());
        prediction.setScoreTeamBPredicted(dto.getScoreTeamBPredicted());
        prediction.setFirstGoalscorerPredicted(dto.getFirstGoalscorerPredicted());
        prediction.setWinningGoalscorerPredicted(dto.getWinningGoalscorerPredicted());
        prediction.setPlayerOfMatchPredicted(dto.getPlayerOfMatchPredicted());
        prediction.setPredictionTime(new Timestamp(System.currentTimeMillis()).toLocalDateTime());
    }

    public static WcUserDto wcUserToDto(WcUser user) {
        WcUserDto dto = new WcUserDto();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setLocation(user.getLocation());
        dto.setIsAdmin(user.getIsAdmin());
        return dto;
    }
}
