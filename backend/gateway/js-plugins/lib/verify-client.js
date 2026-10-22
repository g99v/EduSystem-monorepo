'use strict';

const fs = require('node:fs');
const https = require('node:https');
const tls = require('node:tls');
const { failure, isJson, parseJson, verifiedIdentity, verificationError } = require('./contracts');

function createVerifyClient(config) {

  const rawKey = fs.readFileSync(config.internal_api_key_file);
  if (rawKey.length > 110) {
    throw new Error("Internal API-key configuration is invalid");
  }

  const apiKey = rawKey.toString('utf8').replace(/\r?\n$/, '');
  if (!/^[A-Za-z0-9_-]{1,64}\.[A-Za-z0-9_-]{43}$/.test(apiKey)) {
    throw new Error("Internal API-key configuration is invalid");
  }

  const ca = fs.readFileSync(config.internal_ca_file);
  tls.createSecureContext({ ca, minVersion: 'TLSv1.2' });
  const agent = new https.Agent({ ca, rejectUnauthorized: true, minVersion: 'TLSv1.2',
    keepAlive: true, maxSockets: 16, maxFreeSockets: 4, maxTotalSockets: 16,
    timeout: config.verification_timeout_ms });
  const endpoint = new URL(config.verify_url);

  return (credentials, headers) => new Promise((resolve, reject) => {
    const payload = Buffer.from(JSON.stringify(credentials));
    let request;
    let response;
    let settled = false;
    let timer;
    const finish = (error, identity) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      if (error) {
        request?.destroy();
        response?.destroy();
        reject(error);
      } else {
        resolve(identity);
      }
    };

    timer = setTimeout(() => finish(failure(504, 'Authentication service timed out', 'verify_timeout')),
      config.verification_timeout_ms);
    timer.unref();

    try {
      request = https.request(endpoint, { method: 'POST', agent,
        headers: { ...headers, 'X-Internal-Api-Key': apiKey,
          'Content-Type': 'application/json', Accept: 'application/json',
          'Content-Length': payload.length }, maxHeaderSize: 8192,
      }, upstream => {
        response = upstream;
        if (settled) { upstream.destroy(); return; }
        const status = upstream.statusCode;
        if (status >= 300 && status < 400) {
          finish(failure(502, 'Authentication service returned an invalid response', 'verify_redirect'));
          return;
        }
        if (status >= 500) {
          finish(failure(503, 'Authentication service unavailable', 'verify_upstream'));
          return;
        }
        const declaredLength = Number(upstream.headers['content-length']);
        if (Number.isFinite(declaredLength) && declaredLength > config.max_response_bytes) {
          finish(failure(502, 'Authentication service returned an invalid response', 'verify_response_size'));
          return;
        }
        if (!isJson(upstream.headers['content-type']) || upstream.headers['content-encoding']) {
          finish(failure(502, 'Authentication service returned an invalid response', 'verify_content_type'));
          return;
        }
        const chunks = [];
        let bytes = 0;
        upstream.on('data', chunk => {
          if (settled) return;
          bytes += chunk.length;
          if (bytes > config.max_response_bytes) {
            finish(failure(502, 'Authentication service returned an invalid response', 'verify_response_size'));
          } else { chunks.push(chunk); }
        });
        upstream.on('aborted', () => finish(failure(503, 'Authentication service unavailable', 'verify_network')));
        upstream.on('error', () => finish(failure(503, 'Authentication service unavailable', 'verify_network')));
        upstream.on('end', () => {
          if (settled) return;
          let body;
          try { body = parseJson(Buffer.concat(chunks)); }
          catch { finish(failure(502, 'Authentication service returned an invalid response', 'verify_json')); return; }
          if (status !== 200) { finish(verificationError(status, body)); return; }
          try { finish(null, verifiedIdentity(body, credentials.email)); }
          catch (error) { finish(error); }
        });
      });
      request.on('error', () => finish(failure(503, 'Authentication service unavailable', 'verify_network')));
      request.end(payload);
    } catch {
      finish(failure(503, 'Authentication service unavailable', 'verify_network'));
    }
  });
}

module.exports = { createVerifyClient };
