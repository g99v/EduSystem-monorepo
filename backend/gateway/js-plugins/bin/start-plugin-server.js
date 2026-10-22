#!/usr/bin/env node
'use strict';

async function start() {
  try { require('../lib/runtime').getRuntime(); }
  catch {
    process.stderr.write('JWT issuer signing key, API key or internal TLS configuration is invalid.\n');
    process.exit(1);
  }
  try { await require('../lib/metrics').startMetricsServer(); }
  catch {
    process.stderr.write('JWT issuer metrics listener could not start.\n');
    process.exit(1);
  }
  require('../node_modules/kong-pdk/bin/kong-js-pluginserver');
}

start().catch(() => { process.stderr.write('JWT issuer plugin server could not start.\n'); process.exit(1); });
