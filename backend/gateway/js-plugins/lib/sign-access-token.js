'use strict';

const fs = require('node:fs');
const crypto = require('node:crypto');
const { failure } = require('./contracts');

function createSigner(config) {
  const pem = fs.readFileSync(config.private_key_file);

  const key = crypto.createPrivateKey(pem);

  if (key.asymmetricKeyType !== 'rsa' || key.asymmetricKeyDetails.modulusLength < 2048) {
    throw new Error('JWT signing key must be RSA with at least 2048 bits');
  }

  const publicKey = crypto.createPublicKey(fs.readFileSync(config.public_key_file));
  const derived = crypto.createPublicKey(key).export({ type: 'spki', format: 'der' });
  if (!derived.equals(publicKey.export({ type: 'spki', format: 'der' }))) {
    throw new Error('JWT signing and verification keys do not match');
  }

  return async identity => {
    const now = Math.floor(Date.now() / 1000);
    const header = Buffer.from(JSON.stringify({ alg: 'RS256', typ: 'JWT' })).toString('base64url');
    const claims = Buffer.from(JSON.stringify({
      iss: config.issuer, aud: [config.audience], sub: identity.subject, roles: identity.roles,
      iat: now, nbf: now, exp: now + 600, jti: crypto.randomUUID(),
    })).toString('base64url');

    const input = `${header}.${claims}`;
    let signature;
    try {
      signature = await new Promise((resolve, reject) => {
        crypto.sign('RSA-SHA256', Buffer.from(input, 'ascii'),
          { key, padding: crypto.constants.RSA_PKCS1_PADDING },
          (error, value) => error ? reject(error) : resolve(value));
      });
    } catch {
      throw failure(503, 'Authentication service unavailable', 'signing');
    }
    return `${input}.${signature.toString('base64url')}`;
  };
}

module.exports = { createSigner };
