"""Sync WC 2026 players to prod via API."""
import json
import re
import urllib.request

API_URL = "http://localhost:8080/api/players/sync"
ADMIN_USER = "admin"
WIKI_API = "https://en.wikipedia.org/w/api.php?action=parse&page=2026_FIFA_World_Cup_squads&prop=wikitext&format=json"

POS_MAP = {"GK": "GK", "DF": "DEF", "MF": "MID", "FW": "FWD"}

WIKI_TO_TEAM = {
    "Czech Republic": "Czech Republic", "South Korea": "South Korea",
    "Bosnia and Herzegovina": "Bosnia and Herzegovina",
    "United States": "United States", "Curaçao": "Curaçao",
    "Ivory Coast": "Ivory Coast", "New Zealand": "New Zealand",
    "Cape Verde": "Cape Verde", "Saudi Arabia": "Saudi Arabia",
    "DR Congo": "Democratic Republic of the Congo",
}

TEAMS = {
    "Mexico", "South Africa", "South Korea", "Czech Republic", "Canada",
    "Bosnia and Herzegovina", "Qatar", "Switzerland", "Brazil", "Morocco",
    "Haiti", "Scotland", "United States", "Paraguay", "Australia", "Turkey",
    "Germany", "Curaçao", "Ivory Coast", "Ecuador", "Netherlands", "Japan",
    "Sweden", "Tunisia", "Belgium", "Egypt", "Iran", "New Zealand", "Spain",
    "Cape Verde", "Saudi Arabia", "Uruguay", "France", "Senegal", "Iraq",
    "Norway", "Argentina", "Algeria", "Austria", "Jordan", "Portugal",
    "Democratic Republic of the Congo", "Uzbekistan", "Colombia", "England",
    "Croatia", "Ghana", "Panama"
}


def make_short_name(full_name):
    name = re.sub(r'\s*\([^)]*\)', '', full_name).strip()
    parts = name.split()
    if len(parts) < 2:
        return name
    return parts[0][0] + '. ' + ' '.join(parts[1:])


def scrape_players():
    req = urllib.request.Request(WIKI_API, headers={"User-Agent": "WCPredictionBot/1.0"})
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read())
    wikitext = data["parse"]["wikitext"]["*"]

    players = []
    current_team = None

    for line in wikitext.split("\n"):
        header_match = re.match(r"^===\s*(.+?)\s*===$", line)
        if header_match:
            header = header_match.group(1)
            team_name = WIKI_TO_TEAM.get(header, header)
            current_team = team_name if team_name in TEAMS else None
            continue

        if current_team and "nat fs" in line.lower():
            pos_match = re.search(r"\|pos=(\w+)", line)
            name_match = re.search(r"\|name=\[\[([^|\]]+)", line)
            if not name_match:
                name_match = re.search(r"\|name=([^|}\]]+)", line)
            if pos_match and name_match:
                pos = POS_MAP.get(pos_match.group(1), pos_match.group(1))
                name = name_match.group(1).strip()
                if name:
                    players.append((name, current_team, pos))

    if len(players) < 100:
        players = []
        current_team = None
        current_pos = None
        for line in wikitext.split("\n"):
            header_match = re.match(r"^===\s*(.+?)\s*===$", line)
            if header_match:
                header = header_match.group(1)
                team_name = WIKI_TO_TEAM.get(header, header)
                current_team = team_name if team_name in TEAMS else None
                continue
            if not current_team:
                continue
            pos_match = re.search(r"\[\[(?:Goalkeeper|Defender|Midfielder|Forward)[^|]*\|(\w+)\]\]", line)
            if pos_match:
                current_pos = POS_MAP.get(pos_match.group(1), pos_match.group(1))
            if current_pos and "[[" in line:
                names = re.findall(r"\[\[([^|\]]+?)(?:\|[^\]]+)?\]\]", line)
                for name in names:
                    if any(x in name.lower() for x in [
                        "football", "association", "federation", "captain",
                        "edit", "goalkeeper", "defender", "midfielder", "forward",
                        "born", "aged", "flag"
                    ]):
                        continue
                    if re.search(r"\b(FC|SC|CF|AC|AS|SS|CR|SE|FK|NK|SK|SV|VfB|VfL|RB|TSG|BSC|PFC|1\.|Club|United|City|Real|Inter|Bayern|Borussia|Paris|Sporting|Benfica|Porto|Celtic|Rangers|Arsenal|Chelsea|Liverpool|Juventus|Milan|Roma|Napoli|Atalanta|Bologna|Fiorentina|Torino|Genoa|Parma|Venezia|Como|Pisa|Udinese)\b", name):
                        continue
                    if len(name) > 3 and not name[0].isdigit():
                        players.append((name, current_team, current_pos))
                        break

    seen = set()
    unique = []
    for p in players:
        if (p[0], p[1]) not in seen:
            seen.add((p[0], p[1]))
            unique.append(p)
    return unique


def main():
    players = scrape_players()
    payload = [
        {"playerName": name, "shortName": make_short_name(name), "team": team, "position": pos}
        for name, team, pos in players
    ]

    body = json.dumps(payload).encode()
    req = urllib.request.Request(API_URL, data=body, method="POST", headers={
        "Content-Type": "application/json",
        "X-User-Id": ADMIN_USER,
    })
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
    print(f"Players sync: {result} (scraped {len(players)} players)")


if __name__ == "__main__":
    main()
