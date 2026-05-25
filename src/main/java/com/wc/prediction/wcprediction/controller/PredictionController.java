package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.LeaderboardDTO;
import com.wc.prediction.wcprediction.dto.PredictionDto;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.request.PredictionRequest;
import com.wc.prediction.wcprediction.response.AdminResponse;
import com.wc.prediction.wcprediction.response.PredictionResponse;
import com.wc.prediction.wcprediction.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.wc.prediction.wcprediction.util.CommonUtil.isPredictionAllowed;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;
    @Autowired
    private MatchRepository matchRepository;

    @PostMapping
    public ResponseEntity<PredictionResponse> savePrediction(@RequestBody PredictionDto prediction) {
        PredictionResponse response = new PredictionResponse();
        WcMatch match = matchRepository.findByMatchNo(prediction.getMatchId()).orElse(null);
        if (match == null || !isPredictionAllowed(match.getDateTime())) {
            response.setStatus(false);
            response.setMessage("Prediction is locked for this match");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (prediction.getUserId() == null) {
            response.setStatus(false);
            response.setInvalidUser(true);
            response.setMessage("Login session expired. Please login.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        PredictionDto saved = predictionService.savePrediction(prediction, prediction.getUserId());
        response.setMessage("Prediction saved successfully");
        response.setStatus(true);
        response.setPrediction(saved);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/match")
    public ResponseEntity<PredictionResponse> getPrediction(@RequestBody PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        response.setPrediction(predictionService.getPrediction(request.getUserId(), request.getMatchId()));
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<AdminResponse> getPredictionsByUser(@RequestParam String user) {
        AdminResponse response = new AdminResponse();
        response.setPredictions(predictionService.getPredictionsByUser(user));
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/matches")
    public ResponseEntity<PredictionResponse> getPredictionsForUserByMatches(
            @RequestBody PredictionRequest request) {
        PredictionResponse response = predictionService
                .getPredictionsForUserByMatches(request.getUserId(), request.getMatchIds());
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard(@RequestParam String location) {
        return ResponseEntity.ok(predictionService.getLeaderboard(location));
    }
}
