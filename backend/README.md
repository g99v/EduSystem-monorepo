# Backend and gateway

See the [project README](../README.md) for the stack and first-run instructions.
Run the commands below from `backend`.

## Gateway configuration

Edit `gateway/kong.template.yaml`, then render the local configuration and
recreate Kong:

```sh
python scripts/local-secrets.py render-config
docker compose up -d --force-recreate kong
```

## Rotate local credentials

For an existing database with the application migrations already applied:

```sh
docker compose stop
python scripts/local-secrets.py generate --rotate
docker compose up -d --wait database
python scripts/local-secrets.py provision-db
docker compose up -d --build
python scripts/local-secrets.py reset-grafana
```

This replaces local JWT/TLS keys and database/Grafana passwords, provisions the
new internal API-key verifier, and revokes older Kong service keys. Existing
access tokens become invalid. Database volumes and uploaded files are retained.
Keep `.env`, `secrets/`, and generated `gateway/kong.yaml` local.

For Spring startup outside Docker, supply `SPRING_DATASOURCE_PASSWORD` from your
local `.env`; Spring does not automatically load Compose's environment file.
