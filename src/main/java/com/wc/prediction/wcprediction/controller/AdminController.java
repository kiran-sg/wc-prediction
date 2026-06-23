package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.request.PredictionRequest;
import com.wc.prediction.wcprediction.response.AdminResponse;
import com.wc.prediction.wcprediction.service.AdminService;
import com.wc.prediction.wcprediction.service.EspnScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private EspnScraperService espnScraperService;

    @Autowired
    private MatchRepository matchRepository;

    @GetMapping("/predictions/match")
    public ResponseEntity<AdminResponse> getPredictionsByMatch(@RequestParam String matchId) {
        AdminResponse response = new AdminResponse();
        response.setPredictions(adminService.getPredictionsByMatch(matchId));
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/match/result")
    public ResponseEntity<AdminResponse> getMatchResult(@RequestParam String matchId) {
        AdminResponse response = new AdminResponse();
        response.setMatchResult(adminService.getMatchResult(matchId));
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/match/result")
    public ResponseEntity<AdminResponse> updateMatchResults(@RequestBody MatchResultDto matchResult) {
        return ResponseEntity.ok(adminService.updateMatchResults(matchResult));
    }

    @PostMapping("/prediction/delete")
    public ResponseEntity<AdminResponse> deletePredictions(@RequestBody PredictionRequest request) {
        return ResponseEntity.ok(adminService.deletePredictions(request.getMatchIds()));
    }

    @GetMapping("/match/scrape")
    public ResponseEntity<AdminResponse> scrapeMatchResult(@RequestParam String matchId) {
        AdminResponse response = new AdminResponse();
        WcMatch match = matchRepository.findByMatchNo(matchId).orElse(null);
        if (match == null) {
            response.setStatus(false);
            response.setMessage("Match not found: " + matchId);
            return ResponseEntity.ok(response);
        }
        MatchResultDto scraped = espnScraperService.scrapeMatchResult(match);
        if (scraped == null) {
            response.setStatus(false);
            response.setMessage("Could not fetch result from ESPN. Match may not be finished yet.");
            return ResponseEntity.ok(response);
        }
        response.setStatus(true);
        response.setMatchResult(scraped);
        return ResponseEntity.ok(response);
    }
}
