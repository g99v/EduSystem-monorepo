'use strict';

const http = require('node:http');
const { performance } = require('node:perf_hooks');
const { Registry, Counter, Gauge, Histogram, collectDefaultMetrics } = require('@prometheus-io/client');

// A fixed vocabulary bounds series cardinality and excludes request-controlled labels.
const outcomes = new Set([
  'issued', 'route', 'method', 'configuration', 'origin', 'content_type',
  'request_body', 'request_size', 'request_json', 'request_fields', 'csrf',
  'credentials', 'verify_timeout', 'verify_redirect', 'verify_upstream',
  'verify_response_size', 'verify_content_type', 'verify_network', 'verify_json',
  'verify_contract', 'verify_client_identity', 'verify_status', 'signing', 'plugin',
]);
const buckets = [0.0005, 0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10];
let metrics;

function getMetrics() {
  if (metrics) return metrics;
  const register = new Registry();
  const requests = new Counter({
    name: 'edusystem_jwt_issuer_requests_total',
    help: 'Completed JavaScript JWT issuer access-handler executions by fixed outcome.',
    labelNames: ['outcome'], registers: [register],
  });
  const inFlight = new Gauge({
    name: 'edusystem_jwt_issuer_in_flight',
    help: 'JavaScript JWT issuer access-handler executions currently in flight.',
    registers: [register],
  });
  const duration = new Histogram({
    name: 'edusystem_jwt_issuer_duration_seconds',
    help: 'Access-handler time before PDK logging and response exit, in seconds.',
    buckets, registers: [register],
  });
  const stageDuration = new Histogram({
    name: 'edusystem_jwt_issuer_stage_duration_seconds',
    help: 'Duration of attempted HTTPS verification or RS256 signing, including failures, in seconds.',
    labelNames: ['stage'], buckets, registers: [register],
  });
  for (const outcome of outcomes) requests.labels(outcome).inc(0);
  inFlight.set(0);
  stageDuration.zero({ stage: 'verify' });
  stageDuration.zero({ stage: 'sign' });
  collectDefaultMetrics({ register, prefix: 'edusystem_jwt_issuer_' });
  metrics = { register, requests, inFlight, duration, stageDuration };
  return metrics;
}

function beginAccess() {
  const current = getMetrics();
  const started = performance.now();
  let completed = false;
  current.inFlight.inc();
  return ({ outcome, verificationMs, signingMs, verificationAttempted, signingAttempted }) => {
    if (completed) return;
    completed = true;
    try {
      current.requests.labels(outcomes.has(outcome) ? outcome : 'plugin').inc();
      current.duration.observe(Math.max(0, performance.now() - started) / 1000);
      if (verificationAttempted && Number.isFinite(verificationMs)) {
        current.stageDuration.labels('verify').observe(Math.max(0, verificationMs) / 1000);
      }
      if (signingAttempted && Number.isFinite(signingMs)) {
        current.stageDuration.labels('sign').observe(Math.max(0, signingMs) / 1000);
      }
    } finally {
      current.inFlight.dec();
    }
  };
}

function startMetricsServer() {
  const { register } = getMetrics();
  const server = http.createServer({ maxHeaderSize: 8192, requestTimeout: 5000, headersTimeout: 5000 },
    async (request, response) => {
      response.setHeader('Cache-Control', 'no-store');
      if (request.method !== 'GET') {
        response.writeHead(405, { Allow: 'GET' });
        response.end();
        return;
      }
      if (request.url !== '/metrics') {
        response.writeHead(404);
        response.end();
        return;
      }
      try {
        const body = await register.metrics();
        response.writeHead(200, { 'Content-Type': register.contentType });
        response.end(body);
      } catch {
        response.writeHead(503);
        response.end('Metrics unavailable\n');
      }
    });
  server.on('clientError', (_error, socket) => socket.destroy());
  // This port is internal to Docker; Compose does not publish it on the host.
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(9464, '0.0.0.0', () => resolve(server));
  });
}

// Importing this module for PDK discovery does not create metrics or a listener.
module.exports = { beginAccess, startMetricsServer };
