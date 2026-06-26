package com.wc.prediction.wcprediction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.entity.WcPlayer;
import com.wc.prediction.wcprediction.entity.WcTeam;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.repository.PlayerRepository;
import com.wc.prediction.wcprediction.repository.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class MatchSyncService {

    private static final String ESPN_SCOREBOARD = "https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world/scoreboard?dates=";
    private static final String TEAMS_URL = "https://raw.githubusercontent.com/rezarahiminia/worldcup2026/main/football.teams.json";
    private static final String ESPN_TEAMS_URL = "https://www.espn.com/soccer/teams/_/league/fifa.world";
    private static final String ESPN_SQUAD_URL = "https://www.espn.com/soccer/team/squad/_/id/";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final Map<String, String> POS_MAP = Map.of("G", "GK", "D", "DEF", "M", "MID", "F", "FWD");

    // All dates covering R32 through Final
    private static final List<String> KNOCKOUT_DATES = List.of(
        "20260628","20260629","20260630","20260701","20260702","20260703","20260704",
        "20260705","20260706","20260707","20260708","20260709",
        "20260711","20260712","20260713",
        "20260715","20260716",
        "20260718","20260719"
    );

    private final WebClient webClient = WebClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .defaultHeader("Accept", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    // ── Sync teams from GitHub ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> syncTeams() {
        try {
            String json = webClient.get().uri(TEAMS_URL).retrieve().bodyToMono(String.class).block();
            if (json == null) return Map.of("error", "Could not fetch teams data");
            List<Map<String, Object>> teams = new ObjectMapper().readValue(json, List.class);

            int created = 0, updated = 0;
            for (Map<String, Object> t : teams) {
                String shortName = (String) t.get("fifa_code");
                String teamName  = (String) t.get("name_en");
                String logoUrl   = (String) t.get("flag");

                WcTeam existing = teamRepository.findByShortName(shortName).orElse(null);
                if (existing == null) {
                    WcTeam team = new WcTeam();
                    team.setShortName(shortName);
                    team.setTeamName(teamName);
                    team.setLogoUrl(logoUrl);
                    teamRepository.save(team);
                    created++;
                } else {
                    existing.setTeamName(teamName);
                    existing.setLogoUrl(logoUrl);
                    teamRepository.save(existing);
                    updated++;
                }
            }
            log.info("Teams sync: created={}, updated={}", created, updated);
            return Map.of("created", created, "updated", updated, "total", created + updated);
        } catch (Exception e) {
            log.error("Teams sync failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    // ── Sync players from ESPN squad pages ───────────────────────────────────

    public Map<String, Object> syncPlayers() {
        String html = webClient.get().uri(ESPN_TEAMS_URL).retrieve().bodyToMono(String.class).block();
        if (html == null) return Map.of("error", "Could not fetch ESPN teams page");

        // Extract ESPN team IDs from the page
        Map<String, String> espnTeams = new LinkedHashMap<>();
        Matcher m = Pattern.compile("/soccer/team/_/id/(\\d+)/([a-z-]+)").matcher(html);
        while (m.find()) espnTeams.putIfAbsent(m.group(1), m.group(2).replace("-", " "));

        Map<String, WcTeam> nameToTeam = new HashMap<>();
        teamRepository.findAll().forEach(t -> nameToTeam.put(t.getTeamName().toLowerCase(), t));

        int inserted = 0, updated = 0;
        for (Map.Entry<String, String> entry : espnTeams.entrySet()) {
            String espnId   = entry.getKey();
            String espnName = entry.getValue();
            WcTeam team = findTeamByEspnName(espnName, nameToTeam);
            if (team == null) continue;

            Map<String, WcPlayer> existing = new HashMap<>();
            playerRepository.findByTeam(team.getTeamName())
                    .forEach(p -> existing.put(p.getPlayerName().toLowerCase(), p));

            List<String[]> players = fetchSquad(espnId);
            for (String[] p : players) {
                String name = p[0];
                String pos  = p[1];
                WcPlayer player = existing.get(name.toLowerCase());
                if (player != null) {
                    if (!pos.equals(player.getPosition())) {
                        player.setPosition(pos);
                        playerRepository.save(player);
                        updated++;
                    }
                } else {
                    WcPlayer np = new WcPlayer();
                    np.setPlayerName(name);
                    np.setShortName(makeShortName(name));
                    np.setTeam(team.getTeamName());
                    np.setPosition(pos);
                    playerRepository.save(np);
                    inserted++;
                }
            }
        }
        log.info("Players sync: inserted={}, updated={}", inserted, updated);
        return Map.of("inserted", inserted, "updated", updated, "synced", inserted + updated);
    }

    private List<String[]> fetchSquad(String espnTeamId) {
        try {
            String squadHtml = webClient.get().uri(ESPN_SQUAD_URL + espnTeamId)
                    .retrieve().bodyToMono(String.class).block();
            if (squadHtml == null) return List.of();

            List<String[]> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            Matcher m = Pattern.compile(
                "\"jersey\":\"(\\d+)\"[^}]{0,200}\"name\":\"([^\"]+)\",\"href\":\"[^\"]*soccer/player/_/id/(\\d+)/[^\"]*\"[^}]{0,500}\"position\":\"([GDMF])\""
            ).matcher(squadHtml);
            while (m.find()) {
                String pid = m.group(3);
                String name = m.group(2);
                String pos  = POS_MAP.getOrDefault(m.group(4), "MID");
                if (seen.add(pid)) players.add(new String[]{name, pos});
            }
            if (players.isEmpty()) {
                Matcher m2 = Pattern.compile(
                    "\"name\":\"([^\"]+)\",\"href\":\"[^\"]*soccer/player/_/id/(\\d+)/[^\"]*\"[^}]{0,500}\"position\":\"([GDMF])\""
                ).matcher(squadHtml);
                while (m2.find()) {
                    String pid = m2.group(2);
                    String name = m2.group(1);
                    String pos  = POS_MAP.getOrDefault(m2.group(3), "MID");
                    if (seen.add(pid)) players.add(new String[]{name, pos});
                }
            }
            return players;
        } catch (Exception e) {
            log.warn("Failed squad fetch for ESPN team {}: {}", espnTeamId, e.getMessage());
            return List.of();
        }
    }

    private String makeShortName(String fullName) {
        String name = fullName.replaceAll("\\s*\\([^)]*\\)", "").trim();
        String[] parts = name.split("\\s+");
        if (parts.length < 2) return name;
        return parts[0].charAt(0) + ". " + String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    }

    private WcTeam findTeamByEspnName(String espnName, Map<String, WcTeam> nameToTeam) {
        String lower = espnName.toLowerCase().trim();
        if (nameToTeam.containsKey(lower)) return nameToTeam.get(lower);
        for (Map.Entry<String, WcTeam> e : nameToTeam.entrySet()) {
            if (e.getKey().contains(lower) || lower.contains(e.getKey())) return e.getValue();
        }
        String normalized = normalizeTeamName(lower);
        return nameToTeam.get(normalized);
    }

    public Map<String, Object> syncAll() {
        Map<String, Object> teams   = syncTeams();
        Map<String, Object> matches = syncKnockoutMatches();
        Map<String, Object> players = syncPlayers();
        return Map.of("teams", teams, "matches", matches, "players", players);
    }

    public Map<String, Object> syncKnockoutMatches() {
        ObjectMapper mapper = new ObjectMapper();

        // Delete all existing knockout matches
        List<WcMatch> knockoutMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStage() != null && !m.getStage().equalsIgnoreCase("GROUP"))
                .toList();
        matchRepository.deleteAll(knockoutMatches);
        log.info("Deleted {} existing knockout matches", knockoutMatches.size());

        // Build team lookup by normalised name
        Map<String, WcTeam> nameToTeam = new HashMap<>();
        teamRepository.findAll().forEach(t -> nameToTeam.put(normalizeTeamName(t.getTeamName()), t));

        int inserted = 0;
        int skipped = 0;
        int matchNo = 1;
        for (String date : KNOCKOUT_DATES) {
            try {
                String json = webClient.get().uri(ESPN_SCOREBOARD + date)
                        .retrieve().bodyToMono(String.class).block();
                if (json == null) continue;

                JsonNode events = mapper.readTree(json).path("events");
                for (JsonNode event : events) {
                    JsonNode comp = event.path("competitions").path(0);
                    String dateStr = comp.path("date").asText("");
                    if (dateStr.isBlank()) continue;

                    LocalDateTime matchTime;
                    try {
                        matchTime = OffsetDateTime.parse(dateStr)
                                .atZoneSameInstant(IST)
                                .toLocalDateTime();
                    } catch (Exception e) {
                        continue;
                    }

                    String homeTeamName = null, awayTeamName = null;
                    for (JsonNode competitor : comp.path("competitors")) {
                        String tname = competitor.path("team").path("displayName").asText("");
                        if (competitor.path("homeAway").asText().equals("home")) homeTeamName = tname;
                        else awayTeamName = tname;
                    }
                    if (homeTeamName == null || awayTeamName == null) continue;

                    WcTeam teamA = nameToTeam.get(normalizeTeamName(homeTeamName));
                    WcTeam teamB = nameToTeam.get(normalizeTeamName(awayTeamName));

                    String espnNote = comp.path("notes").path(0).path("headline").asText("").toLowerCase();
                    String stage = resolveStage(espnNote, matchTime);
                    String venue = comp.path("venue").path("fullName").asText("TBD");

                    WcMatch match = new WcMatch();
                    match.setTeamA(teamA != null ? teamA.getTeamName() : homeTeamName);
                    match.setTeamB(teamB != null ? teamB.getTeamName() : awayTeamName);
                    match.setDateTime(matchTime.atZone(IST).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")));
                    match.setVenue(venue);
                    match.setStage(stage);
                    match.setGroupName(stage);
                    match.setMatchNo(String.valueOf(matchNo++));
                    matchRepository.save(match);
                    log.info("Inserted {} match {}: {} vs {} at {}", stage, matchNo - 1, homeTeamName, awayTeamName, matchTime);
                    inserted++;
                }
            } catch (Exception e) {
                log.error("Failed ESPN scoreboard for date {}: {}", date, e.getMessage());
                skipped++;
            }
        }
        return Map.of("deleted", knockoutMatches.size(), "inserted", inserted, "dateErrors", skipped);
    }

    private String resolveStage(String note, LocalDateTime time) {
        if (note.contains("round of 32") || note.contains("r32")) return "R32";
        if (note.contains("round of 16") || note.contains("r16")) return "R16";
        if (note.contains("quarterfinal") || note.contains("quarter-final")) return "QF";
        if (note.contains("semifinal") || note.contains("semi-final")) return "SF";
        if (note.contains("third") || note.contains("3rd")) return "SF";
        if (note.contains("final")) return "FINAL";
        // Fallback by date range
        LocalDate d = time.toLocalDate();
        if (!d.isBefore(LocalDate.of(2026, 6, 28)) && !d.isAfter(LocalDate.of(2026, 7, 4))) return "R32";
        if (!d.isBefore(LocalDate.of(2026, 7, 5)) && !d.isAfter(LocalDate.of(2026, 7, 9))) return "R16";
        if (!d.isBefore(LocalDate.of(2026, 7, 11)) && !d.isAfter(LocalDate.of(2026, 7, 13))) return "QF";
        if (!d.isBefore(LocalDate.of(2026, 7, 15)) && !d.isAfter(LocalDate.of(2026, 7, 16))) return "SF";
        return "FINAL";
    }

    private String normalizeTeamName(String name) {
        if (name == null) return "";
        String s = name.toLowerCase().trim()
                .replaceAll("[áàâãä]", "a").replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i").replaceAll("[óòôõö]", "o")
                .replaceAll("[úùûü]", "u").replaceAll("[ñ]", "n");
        s = s.replace("czechia", "czech republic")
             .replace("turkiye", "turkey").replace("türkiye", "turkey")
             .replace("bosnia-herzegovina", "bosnia and herzegovina")
             .replace("bosnia herzegovina", "bosnia and herzegovina")
             .replace("congo dr", "democratic republic of the congo")
             .replace("cote d'ivoire", "ivory coast").replace("cote divoire", "ivory coast");
        if (s.contains("korea") && !s.contains("south")) s = "south korea";
        if (s.contains("united states") || s.equals("usa")) s = "united states";
        if (s.contains("democratic republic") && s.contains("congo")) s = "democratic republic of the congo";
        return s;
    }
}
