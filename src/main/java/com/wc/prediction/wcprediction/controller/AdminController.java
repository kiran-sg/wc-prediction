package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.repository.*;
import com.wc.prediction.wcprediction.request.PredictionRequest;
import com.wc.prediction.wcprediction.response.AdminResponse;
import com.wc.prediction.wcprediction.service.AdminService;
import com.wc.prediction.wcprediction.service.EspnScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private EspnScraperService espnScraperService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

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

    // DELETE /api/admin/reset?key=users  → clears predictions + users (non-admin)
    // DELETE /api/admin/reset?key=all    → clears teams + players + matches + match results
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(@RequestParam String key) {
        if ("users".equalsIgnoreCase(key)) {
            long predictions = predictionRepository.count();
            long matchResults = matchResultRepository.count();
            long users = userRepository.findAll().stream().filter(u -> !Boolean.TRUE.equals(u.getIsAdmin())).count();
            predictionRepository.deleteAll();
            matchResultRepository.deleteAll();
            userRepository.findAll().stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsAdmin()))
                    .forEach(userRepository::delete);
            return ResponseEntity.ok(Map.of(
                "status", "ok",
                "deleted", Map.of("predictions", predictions, "matchResults", matchResults, "nonAdminUsers", users)
            ));
        } else if ("all".equalsIgnoreCase(key)) {
            long teams = teamRepository.count();
            long players = playerRepository.count();
            long matches = matchRepository.count();
            predictionRepository.deleteAll();
            matchResultRepository.deleteAll();
            matchRepository.deleteAll();
            playerRepository.deleteAll();
            teamRepository.deleteAll();
            return ResponseEntity.ok(Map.of(
                "status", "ok",
                "deleted", Map.of("teams", teams, "players", players, "matches", matches,
                                  "predictionsAndResults", "also cleared")
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid key. Use 'users' or 'all'."
            ));
        }
    }
}
