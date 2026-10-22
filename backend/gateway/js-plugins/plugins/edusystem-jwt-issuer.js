'use strict';

const { randomUUID } = require('node:crypto');
const { performance } = require('node:perf_hooks');
const { AuthFailure, failure, loginRequest, csrfHeaders } = require('../lib/contracts');
const { defaults, getRuntime } = require('../lib/runtime');
const { beginAccess } = require('../lib/metrics');

const responseHeaders = Object.freeze({ 'Content-Type': 'application/json; charset=utf-8',
  'Cache-Control': 'no-store', Pragma: 'no-cache' });

class JwtIssuer {
  constructor(config) {
    try {
      this.runtime = getRuntime(config);
    }
    catch {
      this.configurationFailure = true;
    }
  }

  async access(kong) {
    const correlationId = randomUUID();
    let finishMetrics = () => {};
    try { finishMetrics = beginAccess(); } catch { /* Observability must not change authentication. */ }
    let verificationAttempted = false;
    let signingAttempted = false;
    let verificationMs = 0;
    let signingMs = 0;
    let verificationStart;
    let signingStart;
    let result;
    let outcome = 'issued';
    let extraHeaders = {};

    try {
      if (await kong.request.getPath() !== '/api/auth/login') {
        throw failure(404, 'Not found', 'route');
      }
      const method = await kong.request.getMethod();
      if (method !== 'POST' && method !== 'OPTIONS') {
        extraHeaders = { Allow: 'POST, OPTIONS' };
        throw failure(405, 'Method not allowed', 'method');
      }
      if (this.configurationFailure) {
        throw failure(503, 'Authentication service unavailable', 'configuration');
      }

      const { config, verify, sign } = this.runtime;
      const origin = await kong.request.getHeader('origin');

      if (origin && origin !== config.frontend_origin) {
        throw failure(403, 'Access denied', 'origin');
      }

      const contentType = await kong.request.getHeader('content-type');
      const rawBody = await kong.request.getRawBody();
      const credentials = loginRequest(rawBody, contentType, config.max_request_bytes);
      const cookie = await kong.request.getHeader('cookie');
      const csrfToken = await kong.request.getHeader('x-csrf-token');
      const headers =
          { ...csrfHeaders(cookie, csrfToken), 'X-Correlation-ID': correlationId };

      if (origin) {
        headers.Origin = config.frontend_origin;
      }

      verificationAttempted = true;
      verificationStart = performance.now();

      const identity = await verify(credentials, headers);

      verificationMs = performance.now() - verificationStart;
      verificationStart = undefined;
      signingAttempted = true;
      signingStart = performance.now();

      const accessToken = await sign(identity);

      signingMs = performance.now() - signingStart;
      signingStart = undefined;
      result = { status: 200, body: { accessToken, expiresIn: 600, user: identity.user } };
    } catch (error) {
      const safe = error instanceof AuthFailure ? error
        : failure(503, 'Authentication service unavailable', 'plugin');
      outcome = safe.category;
      if (verificationStart !== undefined) verificationMs = performance.now() - verificationStart;
      if (signingStart !== undefined) signingMs = performance.now() - signingStart;
      result = { status: safe.status, body: safe.body };
    }
    try { finishMetrics({ outcome, verificationMs, signingMs, verificationAttempted, signingAttempted }); }
    catch {}
    try {
      await kong.log.info(JSON.stringify({ plugin: 'edusystem-jwt-issuer', correlationId, outcome,
        verificationMs: Math.round(verificationMs), signingMs: Math.round(signingMs) }));
    } catch {}
    return kong.response.exit(result.status, JSON.stringify(result.body),
      { ...responseHeaders, ...extraHeaders, 'X-Correlation-ID': correlationId });
  }
}

const fixed = (type, value) => ({ type, required: true, default: value, one_of: [value] });

module.exports = {
  Plugin: JwtIssuer, Version: '1.1.0', Priority: 900,
  Schema: [
    { verify_url: fixed('string', defaults.verify_url) },
    { issuer: fixed('string', defaults.issuer) },
    { audience: fixed('string', defaults.audience) },
    { ttl_seconds: fixed('integer', defaults.ttl_seconds) },
    { private_key_file: { type: 'string', required: true, default: defaults.private_key_file } },
    { public_key_file: { type: 'string', required: true, default: defaults.public_key_file } },
    { internal_ca_file: { type: 'string', required: true, default: defaults.internal_ca_file } },
    { internal_api_key_file: { type: 'string', required: true, default: defaults.internal_api_key_file } },
    { verification_timeout_ms: fixed('integer', defaults.verification_timeout_ms) },
    { max_request_bytes: fixed('integer', defaults.max_request_bytes) },
    { max_response_bytes: fixed('integer', defaults.max_response_bytes) },
    { frontend_origin: { type: 'string', required: true, default: defaults.frontend_origin } },
  ],
};
