package com.wc.prediction.wcprediction.service;

import com.wc.prediction.wcprediction.dto.LeaderboardDTO;
import com.wc.prediction.wcprediction.dto.PredictionDto;
import com.wc.prediction.wcprediction.response.PredictionResponse;

import java.util.List;

public interface PredictionService {
    PredictionDto getPrediction(String userId, String matchId);

    PredictionDto savePrediction(PredictionDto predictionDto, String userId);

    List<PredictionDto> getPredictionsByUser(String userId);

    PredictionResponse getPredictionsForUserByMatches(String userId, List<String> matchIds);

    List<LeaderboardDTO> getLeaderboard(String location);
}
