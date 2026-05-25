package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.entity.WcTeam;
import com.wc.prediction.wcprediction.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamRepository teamRepository;

    @GetMapping
    public ResponseEntity<List<WcTeam>> getAllTeams() {
        return ResponseEntity.ok(teamRepository.findAll());
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncTeams(@RequestBody List<WcTeam> teams) {
        int created = 0, updated = 0;
        for (WcTeam team : teams) {
            var existing = teamRepository.findByShortName(team.getShortName());
            if (existing.isEmpty()) {
                teamRepository.save(team);
                created++;
            } else if (team.getLogoUrl() != null && !team.getLogoUrl().isBlank()) {
                WcTeam ex = existing.get();
                ex.setTeamName(team.getTeamName());
                ex.setLogoUrl(team.getLogoUrl());
                teamRepository.save(ex);
                updated++;
            }
        }
        return ResponseEntity.ok(Map.of("created", created, "updated", updated, "total", teams.size()));
    }
}
