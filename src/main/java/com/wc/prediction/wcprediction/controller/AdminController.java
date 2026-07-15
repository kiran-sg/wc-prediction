package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.repository.*;
import com.wc.prediction.wcprediction.request.PredictionRequest;
import com.wc.prediction.wcprediction.response.AdminResponse;
import com.wc.prediction.wcprediction.service.AdminService;
import com.wc.prediction.wcprediction.service.EspnScraperService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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

    @PatchMapping("/users/{userId}/missed-points")
    public ResponseEntity<Map<String, Object>> updateMissedPoints(
            @PathVariable String userId,
            @RequestBody Map<String, Integer> body) {
        WcUser user = userRepository.findByUserId(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "User not found: " + userId));
        }
        Integer points = body.get("missedPoints");
        if (points == null || points < 0) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid missedPoints value"));
        }
        user.setMissedPoints(points);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "ok", "userId", userId, "missedPoints", points));
    }

    // POST /api/admin/users/upload — Excel with columns: Name, Location, Hash ID
    @PostMapping("/users/upload")
    public ResponseEntity<Map<String, Object>> uploadUsers(@RequestParam("file") MultipartFile file) {
        int created = 0, skipped = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Empty file"));

            // Find column indexes by header name (case-insensitive)
            int nameIdx = -1, locationIdx = -1, hashIdx = -1;
            for (Cell cell : header) {
                String h = cell.getStringCellValue().trim().toLowerCase();
                if (h.equals("name"))        nameIdx     = cell.getColumnIndex();
                else if (h.equals("location")) locationIdx = cell.getColumnIndex();
                else if (h.equals("hash id"))  hashIdx     = cell.getColumnIndex();
            }
            if (nameIdx < 0 || locationIdx < 0 || hashIdx < 0) {
                return ResponseEntity.badRequest().body(Map.of("status", "error",
                    "message", "Missing required columns: Name, Location, Hash ID"));
            }

            DataFormatter fmt = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name     = fmt.formatCellValue(row.getCell(nameIdx)).trim();
                String location = fmt.formatCellValue(row.getCell(locationIdx)).trim();
                String userId   = fmt.formatCellValue(row.getCell(hashIdx)).trim();
                if (name.isEmpty() || userId.isEmpty()) { skipped++; continue; }
                if (userRepository.findByUserId(userId) != null) { skipped++; continue; }
                WcUser user = new WcUser();
                user.setName(name);
                user.setLocation(mapLocation(location));
                user.setUserId(userId);
                user.setIsAdmin(false);
                userRepository.save(user);
                created++;
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("status", "ok", "created", created, "skipped", skipped));
    }

    private String mapLocation(String raw) {
        if (raw == null) return "";
        return switch (raw.trim().toLowerCase()) {
            case "trivandrum", "thiruvananthapuram", "tvm" -> "TVM";
            case "pune" -> "Pune";
            default -> raw.trim();
        };
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
