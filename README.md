# EduSystem

An education management learning project for students, instructors, courses,
lectures, assignments, and announcements.

## Stack

- Frontend: React, TypeScript, Vite, and Nginx
- Backend: Java 21, Spring Boot, Spring Security, and Spring Data JPA
- Database: PostgreSQL with Liquibase migrations
- Gateway: Kong with a custom JavaScript JWT issuer
- Monitoring: Prometheus and Grafana
- Containers: Docker Compose

## Requirements

- Docker with Docker Compose v2 and Linux containers
- Python 3.10 or newer (`python3` can be used instead of `python`)
- OpenSSL 1.1.1 or newer, available on PATH

Java and Node.js are installed inside the build containers.

## Run locally

From the repository root, generate the local configuration on the first run:

```sh
cd backend
python scripts/local-secrets.py generate
```

This creates `.env`, local keys/certificates, and `gateway/kong.yaml`.
These files are ignored by Git. If they already exist, skip generation.

Create the Docker resources once. Skip any that already exist:

```sh
docker network create edusystem-gateway
docker volume create postgres_data
docker volume create compose_prometheus_data
docker volume create compose_grafana_data
```

Start the application:

```sh
docker compose up -d --build
```

Wait until `docker compose logs backend` shows `Started EduSystemApplication`.
Then provision the gateway's internal API key in the database:

```sh
python scripts/local-secrets.py provision-db
```

Open the frontend at [http://localhost:5173](http://localhost:5173) and register
a student account. A new database starts without sample courses or users.

| Service | Address |
| --- | --- |
| Frontend | http://localhost:5173 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

Grafana's username and generated password are in `backend/.env`.

## Start and stop

Run these commands from `backend` after the first setup:

```sh
docker compose up -d
# Stop the application while keeping database and uploaded files:
docker compose stop
```

For logs, use `docker compose logs -f`. Configuration changes and credential
rotation are described in [backend/README.md](backend/README.md).
