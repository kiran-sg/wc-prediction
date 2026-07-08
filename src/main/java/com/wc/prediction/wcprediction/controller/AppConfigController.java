package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.entity.AppConfig;
import com.wc.prediction.wcprediction.repository.AppConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppConfigController {

    private static final String TOURNAMENT_OPEN_KEY = "tournament_predictions_open";

    private final AppConfigRepository repo;

    @PostConstruct
    public void init() {
        repo.findByConfigKey(TOURNAMENT_OPEN_KEY).orElseGet(() -> {
            AppConfig c = new AppConfig();
            c.setConfigKey(TOURNAMENT_OPEN_KEY);
            c.setConfigValue(true);
            c.setDescription("Whether tournament predictions are open for users to submit");
            return repo.save(c);
        });
    }

    // Public — called by all users on tournament predict page load
    @GetMapping("/api/config/tournament-open")
    public ResponseEntity<Map<String, Object>> isTournamentOpen() {
        boolean open = repo.findByConfigKey(TOURNAMENT_OPEN_KEY)
            .map(c -> Boolean.TRUE.equals(c.getConfigValue()))
            .orElse(true);
        return ResponseEntity.ok(Map.of("open", open));
    }

    // Admin-only — guarded by AdminInterceptor via /api/admin/**
    @PostMapping("/api/admin/config/tournament-open")
    public ResponseEntity<Map<String, Object>> setTournamentOpen(@RequestBody Map<String, Object> body) {
        boolean open = Boolean.parseBoolean(String.valueOf(body.get("open")));
        AppConfig c = repo.findByConfigKey(TOURNAMENT_OPEN_KEY).orElseGet(() -> {
            AppConfig newC = new AppConfig();
            newC.setConfigKey(TOURNAMENT_OPEN_KEY);
            newC.setDescription("Whether tournament predictions are open for users to submit");
            return newC;
        });
        c.setConfigValue(open);
        repo.save(c);
        return ResponseEntity.ok(Map.of("open", open, "message", "Tournament predictions " + (open ? "opened" : "closed")));
    }
}
