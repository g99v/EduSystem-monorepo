# Gateway monitoring

Prometheus and Grafana run with the application through `backend/compose.yaml`.
Follow the [project setup instructions](../../../README.md) first.

## Dashboard

- Grafana: http://localhost:3000/d/edusystem-gateway
- Prometheus targets: http://localhost:9090/targets
- Grafana credentials: `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD` in
  `backend/.env`

Grafana automatically provisions its data source and gateway/JWT issuer dashboard.
The dashboard includes request counts, status codes, latency, rate-limit responses,
JWT issuer outcomes, and plugin-server process metrics. Sign in through the
frontend to generate traffic; timing graphs can be empty before requests arrive.

## Metrics and logs

Prometheus scrapes Kong at `kong:8100/metrics` and the JavaScript plugin server at
`kong:9464/metrics`. These ports remain inside the Compose network. Grafana and
Prometheus host ports bind to loopback.

From `backend`, view gateway logs with:

```sh
docker compose logs -f --tail 100 kong
```

Prometheus retains up to 15 days or 2 GB of samples. The Prometheus and Grafana
named volumes preserve monitoring data across container recreation.

For an existing Grafana volume, changing `.env` alone does not update its stored
administrator password. Apply the current generated password with:

```sh
python scripts/local-secrets.py reset-grafana
```
