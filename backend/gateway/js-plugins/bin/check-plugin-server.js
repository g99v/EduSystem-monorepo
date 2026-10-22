#!/usr/bin/env node
'use strict';

const net = require('node:net');
const path = require('node:path');
const socket = net.createConnection(path.join(process.env.KONG_PREFIX || '/var/run/kong', 'js_pluginserver.sock'));
socket.setTimeout(1000);
socket.once('connect', () => { socket.destroy(); process.exitCode = 0; });
socket.once('error', () => { socket.destroy(); process.exitCode = 1; });
socket.once('timeout', () => { socket.destroy(); process.exitCode = 1; });
