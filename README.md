# World Cup 2026 Prediction App - Backend

Spring Boot REST API for the World Cup prediction contest.

## Tech Stack

- Java 17, Spring Boot 3.4, Spring Data JPA
- PostgreSQL 16
- Docker Compose

## Quick Start

```bash
# Start database
docker-compose up -d

# Run application
./mvnw spring-boot:run
```

API runs on `http://localhost:8080`

## Database Tables

| Table | Purpose |
|-------|---------|
| `wc_users` | Registered participants |
| `wc_matches` | Match schedule |
| `wc_players` | Player squads per team |
| `wc_predictions` | User predictions + points |
| `wc_match_results` | Actual results (one row per match) |

Tables are auto-created by Hibernate on startup.

## API Endpoints

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/validate` | Login with userId |
| GET | `/api/users` | List all users |
| POST | `/api/users` | Create user |

### Matches
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/matches` | Get all matches (ordered by date) |
| POST | `/api/matches/sync` | Bulk upsert matches |

### Players
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/players` | Get all players |
| GET | `/api/players/team?team=Brazil` | Get players by team |
| GET | `/api/players/teams?teams=Brazil,Argentina` | Get players by multiple teams |

### Predictions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictions` | Submit/update prediction |
| POST | `/api/predictions/match` | Get user's prediction for a match |
| GET | `/api/predictions?user=kiran97` | Get all predictions by user |
| GET | `/api/predictions/leaderboard?location=Mumbai` | Get leaderboard |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/match/result` | Enter/update match result (triggers point calculation) |
| GET | `/api/admin/match/result?matchId=1` | Get existing result |
| GET | `/api/admin/predictions/match?matchId=1` | View all predictions for a match |

## Prediction Categories & Scoring

| Category | Points | Values |
|----------|--------|--------|
| Match Result | 3 | TEAM_A_WIN, TEAM_B_WIN, DRAW |
| Exact Score | 5 | Team A goals – Team B goals |
| First Goalscorer | 5 | Player name or "No Goal" |
| Winning Goalscorer | 5 | Player name or "No Winning Goal (Draw)" |
| Player of the Match | 3 | Player name |

**Max points per match: 21**

## Key Rules

- Predictions lock at kickoff time (server-side enforced)
- Admin entering results auto-calculates points for all users
- Re-submitting a result recalculates points (corrections supported)
- Draw → Winning Goalscorer = "No Winning Goal (Draw)"
- 0-0 → First Goalscorer = "No Goal"

## Configuration

| File | Purpose |
|------|---------|
| `application.properties` | Common config |
| `application-dev.properties` | Local dev (localhost:5432) |
| `application-prod.properties` | Production (env vars) |

## Deployment

```bash
# Build JAR
./mvnw clean package -DskipTests

# Docker
docker build -t wc-prediction .
docker run -p 8080:8080 --env-file .env wc-prediction
```

## Loading Data

Insert matches and players directly into PostgreSQL or use sync endpoints:

```sql
INSERT INTO wc_matches (match_no, date_time, team_a, team_b, group_name, venue)
VALUES ('1', '2026-06-11T21:00:00-04:00', 'Mexico', 'Indonesia', 'A', 'Estadio Azteca');

INSERT INTO wc_players (player_name, team, position)
VALUES ('Vinicius Jr', 'Brazil', 'FWD');

INSERT INTO wc_users (user_id, name, location, is_admin)
VALUES ('kiran97', 'Kiran', 'Mumbai', true);
```
