'use strict';

const { TextDecoder } = require('node:util');

class AuthFailure extends Error {
  constructor(status, body, category) {
    super(category);
    this.status = status;
    this.body = body;
    this.category = category;
  }
}

function failure(status, message, category) {
  return new AuthFailure(status, { message }, category);
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isJson(contentType) {
  return typeof contentType === 'string'
    && contentType.split(';', 1)[0].trim().toLowerCase() === 'application/json';
}

function parseJson(bytes) {
  return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes));
}

function loginRequest(rawBody, contentType, maxBytes) {
  if (!isJson(contentType) || /charset\s*=\s*"?(?!utf-8\b)[\w-]+/i.test(contentType)) {
    throw failure(415, 'Content-Type must be application/json with UTF-8 encoding', 'content_type');
  }
  // The PDK transports binary data as Uint8Array. Older bridges may return a tuple.
  if (Array.isArray(rawBody) && (typeof rawBody[0] === 'string' || rawBody[0] instanceof Uint8Array)) {
    rawBody = rawBody[0];
  }
  if (typeof rawBody !== 'string' && !(rawBody instanceof Uint8Array)) {
    throw failure(400, 'A JSON login body is required', 'request_body');
  }
  const bytes = Buffer.from(rawBody);
  if (bytes.length > maxBytes) {
    throw failure(413, 'Login request is too large', 'request_size');
  }
  let body;
  try {
    body = parseJson(bytes);
  } catch {
    throw failure(400, 'Malformed JSON login body', 'request_json');
  }

  if (!isObject(body)) {
    throw failure(400, 'A JSON login object is required', 'request_body');
  }

  const errors = {};
  if (typeof body.email !== 'string' || body.email.trim().length === 0) {
    errors.email = 'must not be blank';
  }
  if (typeof body.password !== 'string' || body.password.trim().length === 0) {
    errors.password = 'must not be blank';
  }
  if (Object.keys(errors).length) throw new AuthFailure(400, errors, 'request_fields');

  return { email: body.email, password: body.password };
}

function csrfHeaders(cookieHeader, csrfToken) {
  const matches = cookieHeader.split(';').map(part => part.trim())
    .filter(part => part.startsWith('CSRF_TOKEN='));
  if (matches.length !== 1) throw failure(403, 'Access denied', 'csrf');
  const value = matches[0].slice('CSRF_TOKEN='.length);

  return { Cookie: `CSRF_TOKEN=${value}`, 'X-CSRF-TOKEN': csrfToken };
}

function validId(value) {
  return Number.isInteger(value) && value > 0 && value <= 2147483647;
}

function verifiedIdentity(body, requestedEmail) {
  const keys = ['id', 'email', 'role', 'studentId', 'instructorId'];
  if (!isObject(body) || Object.keys(body).length !== keys.length
      || !keys.every(key => Object.hasOwn(body, key)) || !validId(body.id)
      || typeof body.email !== 'string' || body.email.length > 255
      || body.email.toLowerCase() !== requestedEmail.toLowerCase()
      || !['STUDENT', 'INSTRUCTOR', 'ADMIN'].includes(body.role)
      || ![body.studentId, body.instructorId].every(id => id === null || validId(id))
      || (body.role !== 'STUDENT' && body.studentId !== null)
      || (body.role !== 'INSTRUCTOR' && body.instructorId !== null)) {
    throw failure(502, 'Authentication service returned an invalid response', 'verify_contract');
  }
  const user = Object.fromEntries(keys.map(key => [key, body[key]]));
  return { subject: String(user.id), roles: [user.role], user };
}

function verificationError(status, body) {
  if (status === 401 && isObject(body) && body.message === 'Invalid service credential') {
    return failure(503, 'Authentication service unavailable', 'verify_client_identity');
  }
  if (status === 401) return failure(401, 'Invalid email or password', 'credentials');
  if (status === 403 && isObject(body) && body.message === 'Access denied') {
    return failure(403, 'Access denied', 'csrf');
  }
  if (status === 403) return failure(503, 'Authentication service unavailable', 'verify_upstream');
  if (status === 400 && isObject(body)) {
    const allowed = {
      email: ['must not be blank', 'must be a well-formed email address'],
      password: ['must not be blank'],
    };
    const fields = Object.fromEntries(Object.entries(body)
      .filter(([key, value]) => Object.hasOwn(allowed, key) && allowed[key].includes(value)));
    if (Object.keys(fields).length) return new AuthFailure(400, fields, 'request_fields');
  }
  if (status >= 500) return failure(503, 'Authentication service unavailable', 'verify_upstream');
  return failure(502, 'Authentication service returned an invalid response', 'verify_status');
}

module.exports = { AuthFailure, failure, isJson, parseJson, loginRequest, csrfHeaders,
  verifiedIdentity, verificationError };
