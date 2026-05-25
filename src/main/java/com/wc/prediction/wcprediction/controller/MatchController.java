package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.entity.WcTeam;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.repository.TeamRepository;
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
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
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
}
