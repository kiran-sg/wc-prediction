package com.wc.prediction.wcprediction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class EspnScraperService {

    private static final String ESPN_SCOREBOARD_API = "https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world/scoreboard?dates=";

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .defaultHeader("Accept", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    @Autowired
    private PlayerRepository playerRepository;

    public MatchResultDto scrapeMatchResult(WcMatch match) {
        String dateStr = extractDateForEspn(match.getDateTime());
        if (dateStr == null) {
            log.warn("Could not parse date from dateTime: {}", match.getDateTime());
            return null;
        }

        log.info("Fetching ESPN scoreboard for {} vs {} on date {}", match.getTeamA(), match.getTeamB(), dateStr);

        try {
            String json = webClient.get().uri(ESPN_SCOREBOARD_API + dateStr)
                    .retrieve().bodyToMono(String.class).block();
            if (json == null) {
                log.warn("Empty response from ESPN API for date {}", dateStr);
                return null;
            }

            JsonNode events = mapper.readTree(json).path("events");

            // Find the matching event by team names
            String homeTeamId = null, awayTeamId = null;
            JsonNode matchedComp = null;

            for (JsonNode event : events) {
                JsonNode comp = event.path("competitions").path(0);
                String hTeam = null, aTeam = null, hId = null, aId = null;

                for (JsonNode competitor : comp.path("competitors")) {
                    String name = competitor.path("team").path("displayName").asText("");
                    String id   = competitor.path("team").path("id").asText("");
                    if ("home".equals(competitor.path("homeAway").asText())) { hTeam = name; hId = id; }
                    else { aTeam = name; aId = id; }
                }

                if (hTeam != null && aTeam != null
                        && matchesTeam(hTeam, match.getTeamA()) && matchesTeam(aTeam, match.getTeamB())) {
                    matchedComp = comp;
                    homeTeamId  = hId;
                    awayTeamId  = aId;
                    break;
                }
            }

            if (matchedComp == null) {
                log.warn("Could not find {} vs {} in ESPN scoreboard for date {}", match.getTeamA(), match.getTeamB(), dateStr);
                return null;
            }

            // Check completion status
            JsonNode statusType = matchedComp.path("status").path("type");
            boolean completed   = statusType.path("completed").asBoolean(false);
            String  state       = statusType.path("state").asText("");
            if (!completed && !"post".equals(state)) {
                log.warn("Match {} vs {} not finished yet (state={})", match.getTeamA(), match.getTeamB(), state);
                return null;
            }

            // Extract final scores
            int homeScore = 0, awayScore = 0;
            for (JsonNode competitor : matchedComp.path("competitors")) {
                int score = competitor.path("score").asInt(0);
                if ("home".equals(competitor.path("homeAway").asText())) homeScore = score;
                else awayScore = score;
            }
            log.info("ESPN result: {} {}-{} {}", match.getTeamA(), homeScore, awayScore, match.getTeamB());

            // Parse goals from competitions[0].details — each entry has scoringPlay, team.id, ownGoal, penaltyKick, clock, athletesInvolved
            List<GoalEvent> homeGoals = new ArrayList<>();
            List<GoalEvent> awayGoals = new ArrayList<>();

            for (JsonNode detail : matchedComp.path("details")) {
                if (!detail.path("scoringPlay").asBoolean(false)) continue;

                boolean isOwnGoal = detail.path("ownGoal").asBoolean(false);
                boolean isPenalty = detail.path("penaltyKick").asBoolean(false);
                int minute = detail.path("clock").path("value").asInt(0) / 60;
                String scoringTeamId = detail.path("team").path("id").asText("");

                JsonNode athletes = detail.path("athletesInvolved");
                String playerName = athletes.isEmpty() ? "" : athletes.path(0).path("displayName").asText("").trim();

                if (playerName.isEmpty()) continue;

                GoalEvent goal = new GoalEvent(playerName, minute, isPenalty, isOwnGoal);
                if (scoringTeamId.equals(homeTeamId)) homeGoals.add(goal);
                else if (scoringTeamId.equals(awayTeamId)) awayGoals.add(goal);
            }
            log.info("Goal events parsed: home={}, away={}", homeGoals.size(), awayGoals.size());

            return buildDto(match, homeScore, awayScore, homeGoals, awayGoals);

        } catch (Exception e) {
            log.error("ESPN API scrape failed for {} vs {}: {}", match.getTeamA(), match.getTeamB(), e.getMessage(), e);
            return null;
        }
    }

    // IST match time → US Eastern date, matching ESPN's scoreboard bucketing
    private String extractDateForEspn(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return null;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.atZoneSameInstant(ZoneId.of("America/New_York"))
                      .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            Matcher m = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(dateTime);
            return m.find() ? m.group(1) + m.group(2) + m.group(3) : null;
        }
    }

    private MatchResultDto buildDto(WcMatch match, int homeScore, int awayScore,
                                    List<GoalEvent> homeGoals, List<GoalEvent> awayGoals) {
        MatchResultDto dto = new MatchResultDto();
        dto.setMatchId(match.getMatchNo());
        dto.setScoreTeamA(homeScore);
        dto.setScoreTeamB(awayScore);

        if (homeScore > awayScore)      dto.setMatchResult("TEAM_A_WIN");
        else if (awayScore > homeScore) dto.setMatchResult("TEAM_B_WIN");
        else                            dto.setMatchResult("DRAW");

        dto.setWinningGoalscorer(determineWinningGoalscorer(homeScore, awayScore, homeGoals, awayGoals));
        dto.setFirstGoalscorer("");
        dto.setPlayerOfMatch("");
        return dto;
    }

    private String determineWinningGoalscorer(int homeScore, int awayScore,
                                               List<GoalEvent> homeGoals, List<GoalEvent> awayGoals) {
        if (homeScore == awayScore) return "No Winning Goal (Draw)";

        List<GoalEvent> winnerGoals;
        int loserTotal;
        if (homeScore > awayScore) {
            winnerGoals = homeGoals.stream().filter(g -> !g.isOwnGoal())
                    .sorted(Comparator.comparingInt(GoalEvent::minute)).toList();
            loserTotal = awayScore;
        } else {
            winnerGoals = awayGoals.stream().filter(g -> !g.isOwnGoal())
                    .sorted(Comparator.comparingInt(GoalEvent::minute)).toList();
            loserTotal = homeScore;
        }

        // Winning goal = first goal that put winner beyond loser's final total
        for (int i = 0; i < winnerGoals.size(); i++) {
            if ((i + 1) > loserTotal) {
                return resolvePlayerName(winnerGoals.get(i).playerName());
            }
        }

        String raw = winnerGoals.isEmpty() ? "No Winning Goal (Draw)"
                                           : winnerGoals.get(winnerGoals.size() - 1).playerName();
        return resolvePlayerName(raw);
    }

    // ESPN displayName matches DB player_name directly; short-name lookup kept as belt-and-braces
    private String resolvePlayerName(String name) {
        if (name == null || name.startsWith("No ")) return name;
        return playerRepository.findByShortName(name)
                .map(p -> p.getPlayerName())
                .orElse(name);
    }

    private boolean matchesTeam(String espnName, String dbName) {
        String e = espnName.toLowerCase().trim();
        String d = dbName.toLowerCase().trim();
        if (e.equals(d) || e.contains(d) || d.contains(e)) return true;
        if (d.contains("democratic republic") && e.contains("congo")) return true;
        if (d.contains("ivory coast") && (e.contains("ivoire") || e.contains("ivory coast"))) return true;
        if (d.contains("south korea") && e.contains("korea")) return true;
        if (d.contains("united states") && (e.contains("usa") || e.contains("united states"))) return true;
        return false;
    }

    public record GoalEvent(String playerName, int minute, boolean isPenalty, boolean isOwnGoal) {}
}
