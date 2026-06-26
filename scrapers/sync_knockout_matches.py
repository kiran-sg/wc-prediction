"""Sync WC 2026 knockout matches (R32 through Final) from ESPN via API."""
import json
import urllib.request

API_URL = "http://localhost:8080/api/matches/sync-knockout"
ADMIN_USER = "admin"


def main():
    req = urllib.request.Request(API_URL, method="GET", headers={
        "X-User-Id": ADMIN_USER,
    })
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
    print(f"Knockout sync: deleted={result.get('deleted', 0)}, "
          f"inserted={result.get('inserted', 0)}, "
          f"dateErrors={result.get('dateErrors', 0)}")


if __name__ == "__main__":
    main()
