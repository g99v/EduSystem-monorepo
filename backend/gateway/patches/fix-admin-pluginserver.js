const fs = require('node:fs');

// Kong 3.9.1 includes the external plugin server's resty.pipe process handle
// in its configuration. cjson cannot serialize it in the Admin root response.
const target = process.argv[2] || '/usr/local/share/lua/5.1/kong/api/routes/kong.lua';
const source = fs.readFileSync(target, 'utf8');
const marker = '      local configuration = kong.configuration.remove_sensitive()\n';
const replacement = marker +
  '      -- remove_sensitive() deep-copies configuration; retain the live process handle.\n' +
  '      for _, server in ipairs(configuration.pluginservers or {}) do\n' +
  '        server.proc = nil\n' +
  '      end\n';
if (source.split(marker).length !== 2) {
  throw new Error('Kong Admin handler changed; review the plugin-server compatibility patch before building');
}
fs.writeFileSync(target, source.replace(marker, replacement));
console.log('Applied Kong Admin plugin-server serialization fix');
