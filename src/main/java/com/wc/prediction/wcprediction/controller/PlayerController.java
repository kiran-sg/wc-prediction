package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.entity.WcPlayer;
import com.wc.prediction.wcprediction.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping
    public ResponseEntity<List<WcPlayer>> getAllPlayers() {
        return ResponseEntity.ok(playerRepository.findAll());
    }

    @GetMapping("/team")
    public ResponseEntity<List<WcPlayer>> getPlayersByTeam(@RequestParam String team) {
        return ResponseEntity.ok(playerRepository.findByTeam(team));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<WcPlayer>> getPlayersByTeams(@RequestParam List<String> teams) {
        return ResponseEntity.ok(playerRepository.findByTeamIn(teams));
    }
}
