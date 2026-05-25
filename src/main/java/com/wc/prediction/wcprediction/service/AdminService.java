package com.wc.prediction.wcprediction.service;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.dto.PredictionDto;
import com.wc.prediction.wcprediction.response.AdminResponse;

import java.util.List;

public interface AdminService {
    MatchResultDto getMatchResult(String matchId);

    List<PredictionDto> getPredictionsByMatch(String matchId);

    AdminResponse updateMatchResults(MatchResultDto resultDto);

    AdminResponse deletePredictions(List<String> matchIds);
}
