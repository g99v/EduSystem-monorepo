'use strict';

const { createSigner } = require('./sign-access-token');
const { createVerifyClient } = require('./verify-client');

const defaults = Object.freeze({
  verify_url: 'https://backend:8443/internal/auth/verify',
  issuer: 'edusystem-kong', audience: 'edusystem-api', ttl_seconds: 600,
  private_key_file: '/run/secrets/kong_jwt_private_key',
  public_key_file: '/run/secrets/kong_jwt_public_key',
  internal_ca_file: '/run/secrets/internal_ca',
  internal_api_key_file: '/run/secrets/kong_internal_api_key',
  verification_timeout_ms: 5000, max_request_bytes: 8192, max_response_bytes: 16384,
  frontend_origin: 'http://localhost:5173',
});
const runtimes = new Map();

function getRuntime(input = {}) {
  const config = Object.fromEntries(Object.entries(defaults).map(([key, value]) => [key, input[key] ?? value]));
  for (const name of ['verify_url', 'issuer', 'audience', 'ttl_seconds', 'verification_timeout_ms',
    'max_request_bytes', 'max_response_bytes']) {
    if (config[name] !== defaults[name]) throw new Error('Unsupported issuer configuration');
  }
  const origin = new URL(config.frontend_origin);
  if (!['http:', 'https:'].includes(origin.protocol) || origin.origin !== config.frontend_origin) {
    throw new Error('A single frontend origin is required');
  }
  const cacheKey = JSON.stringify(config);
  if (!runtimes.has(cacheKey)) {
    if (runtimes.size >= 8) throw new Error('Restart the plugin server to apply more configurations');
    runtimes.set(cacheKey, Object.freeze({ config,
      sign: createSigner(config), verify: createVerifyClient(config) }));
  }
  return runtimes.get(cacheKey);
}

module.exports = { defaults, getRuntime };
