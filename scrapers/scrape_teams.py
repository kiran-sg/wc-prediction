"""Scrape World Cup 2026 teams from GitHub open-source data and load into PostgreSQL."""
import json
import urllib.request
import psycopg2

DB_CONFIG = {"host": "localhost", "port": 5432, "dbname": "wcprediction", "user": "postgres", "password": "postgres"}
TEAMS_URL = "https://raw.githubusercontent.com/rezarahiminia/worldcup2026/main/football.teams.json"


def main():
    with urllib.request.urlopen(TEAMS_URL) as resp:
        teams = json.loads(resp.read())

    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS wc_teams (
            id BIGSERIAL PRIMARY KEY,
            short_name VARCHAR(10) UNIQUE,
            team_name VARCHAR(100),
            logo_url VARCHAR(500)
        )
    """)

    for t in teams:
        cur.execute("""
            INSERT INTO wc_teams (short_name, team_name, logo_url)
            VALUES (%s, %s, %s)
            ON CONFLICT (short_name) DO UPDATE SET
                team_name = EXCLUDED.team_name, logo_url = EXCLUDED.logo_url
        """, (t["fifa_code"], t["name_en"], t["flag"]))

    conn.commit()
    print(f"Loaded {len(teams)} teams into wc_teams")
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
