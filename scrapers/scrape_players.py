"""Scrape World Cup 2026 players from Wikipedia squads page and load into PostgreSQL."""
import json
import re
import urllib.request
import psycopg2

DB_CONFIG = {"host": "localhost", "port": 5432, "dbname": "wcprediction", "user": "postgres", "password": "postgres"}

# Use Wikipedia API to get raw wikitext (much easier to parse)
API_URL = "https://en.wikipedia.org/w/api.php?action=parse&page=2026_FIFA_World_Cup_squads&prop=wikitext&format=json"

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
    """Generate ESPN-style short name: 'Nizar Al-Rashdan' -> 'N. Al-Rashdan'"""
    # Strip Wikipedia disambiguators like "(footballer)"
    name = re.sub(r'\s*\([^)]*\)', '', full_name).strip()
    parts = name.split()
    if len(parts) < 2:
        return name
    return parts[0][0] + '. ' + ' '.join(parts[1:])


def main():
    req = urllib.request.Request(API_URL, headers={"User-Agent": "WCPredictionBot/1.0"})
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read())

    wikitext = data["parse"]["wikitext"]["*"]

    players = []
    current_team = None

    for line in wikitext.split("\n"):
        # Detect team headers: ===Czech Republic=== or ===Mexico===
        header_match = re.match(r"^===\s*(.+?)\s*===$", line)
        if header_match:
            header = header_match.group(1)
            team_name = WIKI_TO_TEAM.get(header, header)
            if team_name in TEAMS:
                current_team = team_name
            else:
                current_team = None
            continue

        # Squad table rows in wikitext look like:
        # |{{nat fs player|no=|pos=GK|name=[[Player Name]]|...}}
        # or: |{{nat fs g player|...}} for various formats
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

    # If nat_fs template not used, try alternate table format
    # Wikipedia 2026 uses standard wikitables with position column
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

            # Position cells: [[Goalkeeper (association football)|GK]]
            pos_match = re.search(r"\[\[(?:Goalkeeper|Defender|Midfielder|Forward)[^|]*\|(\w+)\]\]", line)
            if pos_match:
                raw_pos = pos_match.group(1)
                current_pos = POS_MAP.get(raw_pos, raw_pos)

            # Player name cells: [[Player Name]] or [[Player Name|Display Name]]
            if current_pos and "[[" in line:
                # Find player links - skip known non-player patterns
                names = re.findall(r"\[\[([^|\]]+?)(?:\|[^\]]+)?\]\]", line)
                for name in names:
                    if any(x in name.lower() for x in [
                        "football", "association", "federation", "captain",
                        "edit", "goalkeeper", "defender", "midfielder", "forward",
                        "born", "aged", "flag"
                    ]):
                        continue
                    # Skip club/country links (usually contain FC, SC, etc.)
                    if re.search(r"\b(FC|SC|CF|AC|AS|SS|CR|SE|FK|NK|SK|SV|VfB|VfL|RB|TSG|BSC|PFC|1\.|Club|United|City|Real|Inter|Bayern|Borussia|Paris|Sporting|Benfica|Porto|Celtic|Rangers|Arsenal|Chelsea|Liverpool|Juventus|Milan|Roma|Napoli|Atalanta|Bologna|Fiorentina|Torino|Genoa|Parma|Venezia|Como|Pisa|Udinese)\b", name):
                        continue
                    if len(name) > 3 and not name[0].isdigit():
                        players.append((name, current_team, current_pos))
                        break  # One player per relevant line

    # Deduplicate
    seen = set()
    unique_players = []
    for p in players:
        key = (p[0], p[1])
        if key not in seen:
            seen.add(key)
            unique_players.append(p)

    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS wc_players (
            id BIGSERIAL PRIMARY KEY,
            player_name VARCHAR(200),
            short_name VARCHAR(100),
            team VARCHAR(100),
            position VARCHAR(10)
        )
    """)
    # Add short_name column if table already exists without it
    cur.execute("""
        DO $$ BEGIN
            ALTER TABLE wc_players ADD COLUMN short_name VARCHAR(100);
        EXCEPTION WHEN duplicate_column THEN NULL;
        END $$;
    """)
    cur.execute("DELETE FROM wc_players")

    for name, team, pos in unique_players:
        short = make_short_name(name)
        cur.execute(
            "INSERT INTO wc_players (player_name, short_name, team, position) VALUES (%s, %s, %s, %s)",
            (name, short, team, pos)
        )

    conn.commit()
    print(f"Loaded {len(unique_players)} players into wc_players")
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
