# EduSystem backend and gateway

Spring Boot/PostgreSQL, a DB-less Kong gateway with a custom JWT issuer, and
Prometheus/Grafana. The React frontend is currently a separate Git repository.

## Local checkout layout

The Compose frontend build uses this layout (clone both repositories):

```
sbtutorial/
  tutorial/                       # this repository; run Compose here
  frontend/edusystem-frontend/     # frontend repository
```

## First setup on a new machine

Install Docker Compose, Python 3, and OpenSSL. Run these commands from `tutorial`:

```
python scripts/local-secrets.py generate
```

The generator creates `.env`, JWT signing keys, an internal service API key,
a local CA and backend TLS certificate, and `gateway/kong.yaml` from the tracked
`gateway/kong.template.yaml`. The generated files are ignored by Git. The TLS
certificate covers the backend's Compose names, localhost, and 127.0.0.1.
These are development credentials; generate separate credentials for AWS.

Review the nonsecret storage/image settings in `.env` before starting. Create
the resources marked external in Compose (only if they do not already exist):

```
docker network create edusystem-gateway
docker volume create postgres_data
docker volume create compose_prometheus_data
docker volume create compose_grafana_data
docker compose up -d --build
python scripts/local-secrets.py provision-db
```

If `POSTGRES_VOLUME` has a different value, create that volume instead of
`postgres_data`. The backend runs Liquibase migrations at startup; provision-db
needs those tables to exist. Kong login may be unavailable until provisioning
finishes. Wait for the backend to finish starting before provisioning.

Open the frontend at http://localhost:5173. Grafana is at http://localhost:3000;
its username and generated password are in local `.env`. Database, gateway
administration, Grafana, and Prometheus host ports bind to loopback. Public HTTPS
and the real frontend origin must be configured before deploying remotely.

## Rotate an existing local installation

Rotation invalidates old access tokens and briefly interrupts authentication.
Preserve your database volumes and uploaded files; do not reset the database.

```
docker compose stop
python scripts/local-secrets.py generate --rotate
docker compose up -d --wait database
python scripts/local-secrets.py provision-db
docker compose up -d --build
python scripts/local-secrets.py reset-grafana
```

The database must already have migration 013's internal-key tables. Provisioning
updates PostgreSQL's actual role password as well as `.env`; changing `.env`
alone does not change an existing database password. It stores only the new
API-key hash and revokes older keys for the Kong identity. Migration 015 also
revokes the originally committed local key on fresh databases. Previously applied
migration 014 is deliberately unchanged to preserve Liquibase checksums.

The Grafana reset command updates its persisted administrator password, which
is not changed by environment variables once the Grafana volume already exists.
Credentials are sent to these local administrative tools through stdin, not
command-line arguments. No credential values are printed by the helper.

## Configuration and Git hygiene

Edit `gateway/kong.template.yaml`, not the generated `gateway/kong.yaml`. To
refresh the rendered config after editing the template without rotating keys,
run `python scripts/local-secrets.py render-config`, then recreate Kong with
`docker compose up -d --force-recreate kong`. Never commit `.env`, `secrets/`, generated gateway
configuration, database exports, or uploaded files. Keep only `.env.example`.

For direct Spring startup outside Docker, set `SPRING_DATASOURCE_PASSWORD` to
the generated database password; Spring does not automatically load Compose's
`.env`. Compose supplies this variable automatically.

Files previously committed remain in Git history. This cleanup rotates active
credentials and removes their files from the next snapshot. Rewriting published
history requires a separate coordinated step across both backend branches and
any other clones; do not merge an old branch that reintroduces secret files.

## Repositories and deployment

Two repositories are supported, provided both checkouts use the layout above.
A single repository would simplify atomic frontend/backend/gateway changes and
one Compose-based deployment, but it is not required for EC2 or CI/CD. The setup
and cleanup do not merge repositories or rewrite history.
