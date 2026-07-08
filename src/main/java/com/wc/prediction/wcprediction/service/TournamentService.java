package com.wc.prediction.wcprediction.service;

import com.wc.prediction.wcprediction.dto.TournamentPredictionDto;
import com.wc.prediction.wcprediction.dto.TournamentResultDto;
import com.wc.prediction.wcprediction.entity.WcUser;

import java.util.List;

public interface TournamentService {
    TournamentPredictionDto getPrediction(String userId);
    TournamentPredictionDto savePrediction(String userId, TournamentPredictionDto dto);
    TournamentResultDto getResult();
    TournamentResultDto saveResult(TournamentResultDto dto);
    void recalculatePoints();
    List<TournamentPredictionDto> getAllPredictions();
    WcUser findUser(String userId);
}
