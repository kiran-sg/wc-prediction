"""Sync WC 2026 teams to prod via API."""
import json
import urllib.request

API_URL = "http://localhost:8080/api/teams/sync"
ADMIN_USER = "admin"
TEAMS_URL = "https://raw.githubusercontent.com/rezarahiminia/worldcup2026/main/football.teams.json"


def main():
    with urllib.request.urlopen(TEAMS_URL) as resp:
        raw = json.loads(resp.read())

    payload = [
        {"shortName": t["fifa_code"], "teamName": t["name_en"], "logoUrl": t["flag"]}
        for t in raw
    ]

    body = json.dumps(payload).encode()
    req = urllib.request.Request(API_URL, data=body, method="POST", headers={
        "Content-Type": "application/json",
        "X-User-Id": ADMIN_USER,
    })
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
    print(f"Teams sync: {result}")


if __name__ == "__main__":
    main()
