"""Create local-only credentials and provision their database verifiers.

Requires Python 3, OpenSSL, and (for provisioning) Docker Compose.
No credential values are printed or passed as command-line arguments.
"""
import argparse
import hashlib
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SECRET_DIR = ROOT / 'secrets'


def read_env(path):
    values = {}
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        if line.strip() and not line.lstrip().startswith('#'):
            key, separator, value = line.partition('=')
            if not separator or not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', key):
                raise ValueError(f'Unsupported environment line in {path.name}')
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in '\"\'':
                value = value[1:-1]
            values[key] = value
    return values


def write_private(path, contents):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(contents, encoding='utf-8', newline='\n')
    path.chmod(0o600)


def run(arguments, *, stdin=None, hide=(), env=None):
    result = subprocess.run(arguments, cwd=ROOT, input=stdin, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env)
    if result.returncode:
        detail = result.stderr.strip()
        for value in hide:
            if value:
                detail = detail.replace(value, '[redacted]')
        raise RuntimeError(f'{arguments[0]} failed: {detail}')
    return result.stdout


def generate(rotate):
    if not shutil.which('openssl'):
        raise RuntimeError('OpenSSL must be installed and available on PATH.')
    existing = [SECRET_DIR / name for name in (
        'kong-jwt-private.pem', 'kong-internal-api-key', 'mtls/ca/ca.key', 'mtls/spring.key')]
    if any(path.exists() for path in existing) and not rotate:
        raise RuntimeError('Credentials already exist. Use generate --rotate to replace them.')
    env_path = ROOT / '.env'
    values = read_env(ROOT / '.env.example')
    if env_path.exists():
        values.update(read_env(env_path))
    values['POSTGRES_PASSWORD'] = secrets.token_urlsafe(32)
    values['GRAFANA_ADMIN_PASSWORD'] = secrets.token_urlsafe(32)
    values.setdefault('GRAFANA_ADMIN_USER', 'admin')
    values.setdefault('AUTH_COOKIE_SECURE', 'false')
    key_id = 'kong-local-' + secrets.token_hex(8)
    raw_key = key_id + '.' + secrets.token_urlsafe(32)
    SECRET_DIR.mkdir(exist_ok=True)
    if os.name != 'nt':
        SECRET_DIR.chmod(0o700)
    # Generate and validate everything before replacing any active local file.
    with tempfile.TemporaryDirectory(prefix='.generate-', dir=SECRET_DIR) as temporary:
        stage = Path(temporary)
        # Some Windows OpenSSL builds embed a nonexistent default config path.
        (stage / 'openssl.cnf').write_text('[req]\ndistinguished_name=dn\n[dn]\n')
        openssl_env = dict(os.environ, OPENSSL_CONF=str(stage / 'openssl.cnf'))
        def openssl(*arguments):
            return run(['openssl', *map(str, arguments)], env=openssl_env)
        openssl('genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:3072',
                '-out', stage / 'jwt-private.pem')
        openssl('pkey', '-in', stage / 'jwt-private.pem', '-pubout', '-out', stage / 'jwt-public.pem')
        openssl('genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:3072',
                '-out', stage / 'ca.key')
        openssl('req', '-x509', '-new', '-sha256', '-days', '365', '-key', stage / 'ca.key',
                '-subj', '/CN=EduSystem Local Development CA',
                '-addext', 'basicConstraints=critical,CA:TRUE,pathlen:0',
                '-addext', 'keyUsage=critical,keyCertSign,cRLSign', '-out', stage / 'ca.pem')
        openssl('genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:3072',
                '-out', stage / 'spring.key')
        openssl('req', '-new', '-sha256', '-key', stage / 'spring.key',
                '-subj', '/CN=backend', '-out', stage / 'spring.csr')
        (stage / 'spring.ext').write_text(
            'basicConstraints=critical,CA:FALSE\n'
            'keyUsage=critical,digitalSignature,keyEncipherment\n'
            'extendedKeyUsage=serverAuth\n'
            'subjectAltName=DNS:backend,DNS:edusystem-upstream,DNS:localhost,IP:127.0.0.1\n')
        openssl('x509', '-req', '-in', stage / 'spring.csr', '-CA', stage / 'ca.pem',
                '-CAkey', stage / 'ca.key', '-set_serial', str(secrets.randbits(128) or 1),
                '-days', '365', '-sha256', '-extfile', stage / 'spring.ext', '-out', stage / 'spring.pem')
        openssl('verify', '-CAfile', stage / 'ca.pem', '-verify_hostname', 'backend', stage / 'spring.pem')
        config = (ROOT / 'gateway/kong.template.yaml').read_text()
        for placeholder, source, indent in (
            ('__JWT_PUBLIC_KEY_PEM__', 'jwt-public.pem', '          '),
            ('__INTERNAL_CA_PEM__', 'ca.pem', '      '),
        ):
            marker = indent + placeholder
            if config.count(marker) != 1:
                raise ValueError('Expected exactly one ' + placeholder + ' placeholder')
            config = config.replace(marker, '\n'.join(indent + line for line in (stage / source).read_text().splitlines()))
        files = {
            'kong-jwt-private.pem': 'jwt-private.pem', 'kong-jwt-public.pem': 'jwt-public.pem',
            'mtls/ca/ca.key': 'ca.key', 'mtls/ca.pem': 'ca.pem',
            'mtls/spring.key': 'spring.key', 'mtls/spring.pem': 'spring.pem',
            'mtls/spring.csr': 'spring.csr', 'mtls/spring.ext': 'spring.ext',
        }
        for destination, source in files.items():
            write_private(SECRET_DIR / destination, (stage / source).read_text())
        write_private(SECRET_DIR / 'kong-internal-api-key', raw_key + '\n')
        write_private(env_path, '\n'.join(f'{key}={value}' for key, value in values.items()) + '\n')
        (ROOT / 'gateway/kong.yaml').write_text(config, encoding='utf-8', newline='\n')
    print('Generated local JWT keys, CA/server TLS keys, service API key, and database/Grafana passwords.')
    print('Values are only in ignored .env and secrets/. Generated gateway/kong.yaml is also ignored.')
    print('Existing databases still need provision-db; existing Grafana storage needs reset-grafana.')


def render_config():
    config = (ROOT / 'gateway/kong.template.yaml').read_text()
    for placeholder, source, indent in (
        ('__JWT_PUBLIC_KEY_PEM__', SECRET_DIR / 'kong-jwt-public.pem', '          '),
        ('__INTERNAL_CA_PEM__', SECRET_DIR / 'mtls/ca.pem', '      '),
    ):
        marker = indent + placeholder
        if config.count(marker) != 1:
            raise ValueError('Expected exactly one ' + placeholder + ' placeholder')
        config = config.replace(marker, '\n'.join(indent + line for line in source.read_text().splitlines()))
    (ROOT / 'gateway/kong.yaml').write_text(config, encoding='utf-8', newline='\n')
    print('Rendered gateway/kong.yaml using the current local public key and CA.')


def sql_string(value):
    return "'" + value.replace("'", "''") + "'"


def provision_db():
    values = read_env(ROOT / '.env')
    password = values['POSTGRES_PASSWORD']
    raw_key = (SECRET_DIR / 'kong-internal-api-key').read_text().strip()
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,64}\.[A-Za-z0-9_-]{43}', raw_key):
        raise ValueError('Malformed local service credential')
    if not password:
        raise ValueError('Generate a database password first')
    key_id = raw_key.split('.')[0]
    digest = hashlib.sha256(raw_key.encode()).hexdigest()
    role = '"' + values['POSTGRES_USER'].replace('"', '""') + '"'
    sql = f'''BEGIN;
ALTER ROLE {role} WITH PASSWORD {sql_string(password)};
INSERT INTO t_internal_api_key (key_id, client_id, key_hash, enabled, revoked)
VALUES ({sql_string(key_id)}, 'kong', decode('{digest}', 'hex'), TRUE, FALSE)
ON CONFLICT (key_id) DO UPDATE SET key_hash = EXCLUDED.key_hash, enabled = TRUE, revoked = FALSE;
INSERT INTO t_internal_api_key_authority (key_id, authority)
VALUES ({sql_string(key_id)}, 'INTERNAL_AUTH_VERIFY') ON CONFLICT DO NOTHING;
UPDATE t_internal_api_key SET enabled = FALSE, revoked = TRUE
WHERE client_id = 'kong' AND key_id <> {sql_string(key_id)};
COMMIT;
'''
    # Uses the container's local socket, not a password in an argument or a network connection.
    # PostgreSQL's official image allows this local administrative connection by default.
    run(['docker', 'compose', 'exec', '-T', 'database', 'psql', '-U', values['POSTGRES_USER'],
         '-d', values['POSTGRES_DB'], '-v', 'ON_ERROR_STOP=1'], stdin=sql, hide=(password, raw_key))
    print('Applied the database password and new Kong verifier; revoked older Kong credentials.')
    print('Recreate backend and Kong to load the current configuration and key files.')


def reset_grafana():
    values = read_env(ROOT / '.env')
    password = values['GRAFANA_ADMIN_PASSWORD']
    if not password:
        raise ValueError('Generate a Grafana password first')
    run(['docker', 'compose', 'exec', '-T', 'grafana', 'grafana', 'cli',
         '--homepath', '/usr/share/grafana', 'admin', 'reset-admin-password', '--password-from-stdin'],
        stdin=password + '\n', hide=(password,))
    print('Updated the persisted Grafana administrator password to the value in local .env.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    generate_parser = commands.add_parser('generate', help='Generate ignored local credential/configuration files')
    generate_parser.add_argument('--rotate', action='store_true', help='Replace existing local credentials')
    commands.add_parser('render-config', help='Render gateway configuration without rotating credentials')
    commands.add_parser('provision-db', help='Apply local database credentials after migrations exist')
    commands.add_parser('reset-grafana', help='Apply the generated password to existing Grafana storage')
    args = parser.parse_args()
    try:
        if args.command == 'generate':
            generate(args.rotate)
        elif args.command == 'render-config':
            render_config()
        elif args.command == 'provision-db':
            provision_db()
        else:
            reset_grafana()
    except (OSError, ValueError, RuntimeError, KeyError) as error:
        print('Setup failed: ' + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
