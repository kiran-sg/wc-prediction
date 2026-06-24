package com.wc.prediction.wcprediction.service;

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

    private static final String ESPN_SCOREBOARD = "https://www.espn.com/soccer/scoreboard/_/league/fifa.world/date/";
    private final WebClient webClient = WebClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    @Autowired
    private PlayerRepository playerRepository;

    public MatchResultDto scrapeMatchResult(WcMatch match) {
        String dateStr = extractDate(match.getDateTime());
        if (dateStr == null) {
            log.warn("Could not parse date from dateTime: {}", match.getDateTime());
            return null;
        }

        String url = ESPN_SCOREBOARD + dateStr;
        log.info("Scraping ESPN: {}", url);

        try {
            String html = webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
            if (html == null || html.isEmpty()) {
                log.warn("Empty response from ESPN for {}", url);
                return null;
            }

            return extractMatchResult(html, match);

        } catch (Exception e) {
            log.error("ESPN scrape failed for {} vs {}: {}", match.getTeamA(), match.getTeamB(), e.getMessage(), e);
            return null;
        }
    }

    private MatchResultDto extractMatchResult(String html, WcMatch match) {
        List<String> teamNames = extractAll(html, "ScoreCell__TeamName ScoreCell__TeamName--shortDisplayName db\\\">([^<]+)</div>");
        List<String> scores = extractAll(html, "ScoreCell__Score[^>]*>([0-9]+)</div>");

        log.info("ESPN page: found {} teams, {} scores", teamNames.size(), scores.size());

        for (int i = 0; i < teamNames.size() - 1; i += 2) {
            String espnHome = teamNames.get(i);
            String espnAway = teamNames.get(i + 1);

            if (matchesTeam(espnHome, match.getTeamA()) && matchesTeam(espnAway, match.getTeamB())) {
                if (i + 1 >= scores.size()) {
                    log.warn("Scores not available yet for {} vs {}", espnHome, espnAway);
                    return null;
                }

                int homeScore = Integer.parseInt(scores.get(i));
                int awayScore = Integer.parseInt(scores.get(i + 1));

                int matchIdx = i / 2;
                List<GoalEvent> homeGoals = extractGoals(html, matchIdx, 0);
                List<GoalEvent> awayGoals = extractGoals(html, matchIdx, 1);

                log.info("Scraped: {} {}-{} {} | homeGoals={}, awayGoals={}",
                        espnHome, homeScore, awayScore, espnAway, homeGoals.size(), awayGoals.size());

                return buildDto(match, homeScore, awayScore, homeGoals, awayGoals);
            }
        }

        log.warn("Could not find {} vs {} on ESPN page", match.getTeamA(), match.getTeamB());
        return null;
    }

    private List<GoalEvent> extractGoals(String html, int matchIdx, int teamOffset) {
        String perfMarker = "SoccerPerformers__Competitor__Team__Name\">";
        List<Integer> positions = new ArrayList<>();
        int from = 0;
        while (true) {
            int pos = html.indexOf(perfMarker, from);
            if (pos < 0) break;
            positions.add(pos);
            from = pos + perfMarker.length();
        }

        int idx = matchIdx * 2 + teamOffset;
        if (idx >= positions.size()) return Collections.emptyList();

        int start = positions.get(idx);
        int end = (idx + 1) < positions.size() ? positions.get(idx + 1) : start + 2000;
        return parseGoalsFromSection(html.substring(start, Math.min(end, html.length())));
    }

    private List<GoalEvent> parseGoalsFromSection(String section) {
        List<GoalEvent> goals = new ArrayList<>();
        section = section.replace("&#x27;", "'").replace("&amp;", "&").replace("<!-- -->", "");
        Pattern p = Pattern.compile("Soccer__PlayerName[^>]*>([^<]+)</a>.*?GoalScore__Time\">([^<]+)</span>", Pattern.DOTALL);
        Matcher m = p.matcher(section);
        while (m.find()) {
            String playerName = m.group(1).trim();
            String timeStr = m.group(2).replaceAll("[\\s-]+", " ").trim();
            for (String part : timeStr.split(",")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                goals.add(new GoalEvent(playerName, parseMinute(part), part.contains("Pen"), part.contains("OG")));
            }
        }
        return goals;
    }

    private MatchResultDto buildDto(WcMatch match, int homeScore, int awayScore,
                                    List<GoalEvent> homeGoals, List<GoalEvent> awayGoals) {
        MatchResultDto dto = new MatchResultDto();
        dto.setMatchId(match.getMatchNo());
        dto.setScoreTeamA(homeScore);
        dto.setScoreTeamB(awayScore);

        if (homeScore > awayScore) dto.setMatchResult("TEAM_A_WIN");
        else if (awayScore > homeScore) dto.setMatchResult("TEAM_B_WIN");
        else dto.setMatchResult("DRAW");

        // Winning goalscorer: the goal that first put the winner beyond the loser's final total
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

        // The winning goal is when winner's running count first exceeds the loser's final total
        for (int i = 0; i < winnerGoals.size(); i++) {
            if ((i + 1) > loserTotal) {
                return resolvePlayerName(winnerGoals.get(i).playerName());
            }
        }

        String raw = winnerGoals.isEmpty() ? "No Winning Goal (Draw)" : winnerGoals.get(winnerGoals.size() - 1).playerName();
        return resolvePlayerName(raw);
    }

    // Resolve ESPN short name (e.g. "N. Al-Rashdan") to full DB player name via short_name column
    private String resolvePlayerName(String espnName) {
        if (espnName == null || espnName.startsWith("No ")) return espnName;
        return playerRepository.findByShortName(espnName)
                .map(p -> p.getPlayerName())
                .orElse(espnName);
    }

    private String extractDate(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return null;
        try {
            // ESPN scoreboard pages are bucketed by US Eastern time (EDT = UTC-4)
            OffsetDateTime odt = OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.atZoneSameInstant(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            Matcher m = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(dateTime);
            return m.find() ? m.group(1) + m.group(2) + m.group(3) : null;
        }
    }

    private int parseMinute(String minuteStr) {
        Matcher m = Pattern.compile("(\\d+)").matcher(minuteStr);
        return m.find() ? Integer.parseInt(m.group(1)) : 999;
    }

    private List<String> extractAll(String text, String regex) {
        List<String> results = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) results.add(m.group(1));
        return results;
    }

    private boolean matchesTeam(String espnName, String dbName) {
        String e = espnName.toLowerCase().trim();
        String d = dbName.toLowerCase().trim();
        if (e.equals(d) || e.contains(d) || d.contains(e)) return true;
        if (d.contains("democratic republic") && e.contains("congo")) return true;
        if (d.contains("ivory coast") && e.contains("ivoire")) return true;
        if (d.contains("south korea") && e.contains("korea")) return true;
        if (d.contains("united states") && (e.contains("usa") || e.contains("united states"))) return true;
        return false;
    }

    public record GoalEvent(String playerName, int minute, boolean isPenalty, boolean isOwnGoal) {}
}
