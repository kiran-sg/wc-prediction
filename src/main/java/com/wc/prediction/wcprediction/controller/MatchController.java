package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.entity.WcTeam;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.repository.MatchResultRepository;
import com.wc.prediction.wcprediction.repository.TeamRepository;
import com.wc.prediction.wcprediction.response.AdminResponse;
import com.wc.prediction.wcprediction.service.MatchSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private MatchSyncService matchSyncService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllMatches() {
        List<WcMatch> matches = matchRepository.findAllOrderByDateTime();
        Map<String, String> logoCache = teamRepository.findAll().stream()
                .collect(Collectors.toMap(WcTeam::getTeamName, WcTeam::getLogoUrl, (a, b) -> a));

        List<Map<String, Object>> result = matches.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("matchNo", m.getMatchNo());
            map.put("dateTime", m.getDateTime());
            map.put("teamA", m.getTeamA());
            map.put("teamB", m.getTeamB());
            map.put("teamALogo", logoCache.getOrDefault(m.getTeamA(), ""));
            map.put("teamBLogo", logoCache.getOrDefault(m.getTeamB(), ""));
            map.put("groupName", m.getGroupName());
            map.put("venue", m.getVenue());
            map.put("stage", m.getStage());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/result")
    public ResponseEntity<AdminResponse> getMatchResult(@RequestParam String matchId) {
        AdminResponse response = new AdminResponse();
        matchResultRepository.findByMatchId(matchId).ifPresent(r -> {
            MatchResultDto dto = new MatchResultDto();
            dto.setMatchId(r.getMatchId());
            dto.setMatchResult(r.getMatchResult());
            dto.setScoreTeamA(r.getScoreTeamA());
            dto.setScoreTeamB(r.getScoreTeamB());
            dto.setFirstGoalscorer(r.getFirstGoalscorer());
            dto.setWinningGoalscorer(r.getWinningGoalscorer());
            dto.setPlayerOfMatch(r.getPlayerOfMatch());
            response.setMatchResult(dto);
        });
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncMatches(@RequestBody List<WcMatch> matches) {
        int created = 0, updated = 0;
        for (WcMatch incoming : matches) {
            WcMatch existing = matchRepository.findByMatchNo(incoming.getMatchNo()).orElse(null);
            if (existing != null) {
                existing.setDateTime(incoming.getDateTime());
                existing.setTeamA(incoming.getTeamA());
                existing.setTeamB(incoming.getTeamB());
                existing.setGroupName(incoming.getGroupName());
                existing.setVenue(incoming.getVenue());
                matchRepository.save(existing);
                updated++;
            } else {
                matchRepository.save(incoming);
                created++;
            }
        }
        return ResponseEntity.ok(Map.of("created", created, "updated", updated, "total", matches.size()));
    }

    @GetMapping("/sync-knockout")
    public ResponseEntity<Map<String, Object>> syncKnockoutMatches() {
        return ResponseEntity.ok(matchSyncService.syncKnockoutMatches());
    }

    @GetMapping("/sync-teams")
    public ResponseEntity<Map<String, Object>> syncTeams() {
        return ResponseEntity.ok(matchSyncService.syncTeams());
    }

    @GetMapping("/sync-players")
    public ResponseEntity<Map<String, Object>> syncPlayers() {
        return ResponseEntity.ok(matchSyncService.syncPlayers());
    }

    @GetMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        return ResponseEntity.ok(matchSyncService.syncAll());
    }
}
