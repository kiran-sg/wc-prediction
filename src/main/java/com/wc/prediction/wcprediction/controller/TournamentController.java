package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.TournamentPredictionDto;
import com.wc.prediction.wcprediction.dto.TournamentResultDto;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.service.TournamentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournament")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    /** GET /api/tournament/prediction?userId=xxx — caller must be the owner */
    @GetMapping("/prediction")
    public ResponseEntity<Map<String, Object>> getPrediction(@RequestParam String userId,
                                                              HttpServletRequest request) {
        String callerId = request.getHeader("X-User-Id");
        if (callerId == null || !callerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        TournamentPredictionDto pred = tournamentService.getPrediction(userId);
        return ResponseEntity.ok(Map.of("prediction", pred != null ? pred : Map.of()));
    }

    /** POST /api/tournament/prediction  body: {userId, goldenBall, goldenBoot, ...} */
    @PostMapping("/prediction")
    public ResponseEntity<Map<String, Object>> savePrediction(@Valid @RequestBody TournamentPredictionDto dto,
                                                               HttpServletRequest request) {
        String callerId = request.getHeader("X-User-Id");
        if (callerId == null || !callerId.equals(dto.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        TournamentPredictionDto saved = tournamentService.savePrediction(dto.getUserId(), dto);
        return ResponseEntity.ok(Map.of("status", true, "message", "Tournament prediction saved", "prediction", saved));
    }

    /** GET /api/tournament/result */
    @GetMapping("/result")
    public ResponseEntity<TournamentResultDto> getResult() {
        return ResponseEntity.ok(tournamentService.getResult());
    }

    /** POST /api/tournament/result  (admin only) */
    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> saveResult(@Valid @RequestBody TournamentResultDto dto,
                                                           HttpServletRequest request) {
        String callerId = request.getHeader("X-User-Id");
        if (callerId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        WcUser caller = tournamentService.findUser(callerId);
        if (caller == null || !Boolean.TRUE.equals(caller.getIsAdmin())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        TournamentResultDto saved = tournamentService.saveResult(dto);
        return ResponseEntity.ok(Map.of("status", true, "message", "Tournament result saved and points recalculated", "result", saved));
    }

    /** GET /api/tournament/predictions  (admin only) */
    @GetMapping("/predictions")
    public ResponseEntity<List<TournamentPredictionDto>> getAllPredictions() {
        return ResponseEntity.ok(tournamentService.getAllPredictions());
    }
}
