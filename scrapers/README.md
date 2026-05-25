# World Cup 2026 Data Scrapers

Python scripts to load FIFA World Cup 2026 data into PostgreSQL.

## Setup

```bash
pip install -r requirements.txt
```

## Data Sources

| Script | Source | Table |
|--------|--------|-------|
| `scrape_matches.py` | [rezarahiminia/worldcup2026](https://github.com/rezarahiminia/worldcup2026) JSON | `wc_matches` |
| `scrape_teams.py` | Same GitHub repo | `wc_teams` |
| `scrape_players.py` | [Wikipedia WC 2026 squads](https://en.wikipedia.org/wiki/2026_FIFA_World_Cup_squads) | `wc_players` |

## Usage

```bash
# Ensure PostgreSQL is running (docker compose up -d)
python scrape_teams.py
python scrape_matches.py
python scrape_players.py
```

Run teams first since matches reference team names.
