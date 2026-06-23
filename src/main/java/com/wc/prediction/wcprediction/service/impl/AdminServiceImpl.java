package com.wc.prediction.wcprediction.service.impl;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.dto.PredictionDto;
import com.wc.prediction.wcprediction.entity.MatchResult;
import com.wc.prediction.wcprediction.entity.Prediction;
import com.wc.prediction.wcprediction.repository.MatchResultRepository;
import com.wc.prediction.wcprediction.repository.PredictionRepository;
import com.wc.prediction.wcprediction.response.AdminResponse;
import com.wc.prediction.wcprediction.service.AdminService;
import com.wc.prediction.wcprediction.util.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Override
    public MatchResultDto getMatchResult(String matchId) {
        return matchResultRepository.findByMatchId(matchId)
                .map(r -> {
                    MatchResultDto dto = new MatchResultDto();
                    dto.setMatchId(r.getMatchId());
                    dto.setMatchResult(r.getMatchResult());
                    dto.setScoreTeamA(r.getScoreTeamA());
                    dto.setScoreTeamB(r.getScoreTeamB());
                    dto.setFirstGoalscorer(r.getFirstGoalscorer());
                    dto.setWinningGoalscorer(r.getWinningGoalscorer());
                    dto.setPlayerOfMatch(r.getPlayerOfMatch());
                    return dto;
                })
                .orElse(null);
    }

    @Override
    public List<PredictionDto> getPredictionsByMatch(String matchId) {
        List<PredictionDto> dtoList = new ArrayList<>();
        Optional<List<Prediction>> predictions = predictionRepository.findAllByMatchId(matchId);
        predictions.ifPresent(list -> list.forEach(p ->
                dtoList.add(MapperUtil.predictionToPredictionDto(p))));
        return dtoList;
    }

    @Override
    public AdminResponse updateMatchResults(MatchResultDto resultDto) {
        AdminResponse response = new AdminResponse();

        // Save/update result in wc_match_results
        MatchResult result = matchResultRepository.findByMatchId(resultDto.getMatchId())
                .orElse(new MatchResult());
        result.setMatchId(resultDto.getMatchId());
        result.setMatchResult(resultDto.getMatchResult());
        result.setScoreTeamA(resultDto.getScoreTeamA());
        result.setScoreTeamB(resultDto.getScoreTeamB());
        result.setFirstGoalscorer(resultDto.getFirstGoalscorer());
        result.setWinningGoalscorer(resultDto.getWinningGoalscorer());
        result.setPlayerOfMatch(resultDto.getPlayerOfMatch());
        result.setResultUpdatedTime(new Timestamp(System.currentTimeMillis()).toLocalDateTime());
        matchResultRepository.save(result);

        // Calculate points for all predictions of this match
        Optional<List<Prediction>> predictions = predictionRepository.findAllByMatchId(resultDto.getMatchId());
        predictions.ifPresent(list -> {
            list.forEach(p -> p.setPoints(calculatePoints(p, result)));
            predictionRepository.saveAll(list);
        });

        response.setMessage("Results updated for Match " + resultDto.getMatchId());
        response.setStatus(true);
        return response;
    }

    @Override
    public AdminResponse deletePredictions(List<String> matchIds) {
        AdminResponse response = new AdminResponse();
        Optional<List<Prediction>> predictions = predictionRepository.findAllByMatchIdIn(matchIds);
        if (predictions.isPresent()) {
            predictionRepository.deleteAll(predictions.get());
            response.setStatus(true);
            response.setMessage("Predictions deleted for matches: " + matchIds);
        }
        return response;
    }

    private int calculatePoints(Prediction prediction, MatchResult result) {
        int points = 0;

        // Q1 — Who progresses: 3 pts
        if (prediction.getMatchResultPredicted().equals(result.getMatchResult())) {
            points += 3;
        }
        // Q2 — Exact score after full time / extra time: 5 pts
        if (prediction.getScoreTeamAPredicted().equals(result.getScoreTeamA())
                && prediction.getScoreTeamBPredicted().equals(result.getScoreTeamB())) {
            points += 5;
        }
        // Q3 — Winning goalscorer: 5 pts
        if (prediction.getWinningGoalscorerPredicted() != null
                && prediction.getWinningGoalscorerPredicted().equals(result.getWinningGoalscorer())) {
            points += 5;
        }

        return points;
    }
}
