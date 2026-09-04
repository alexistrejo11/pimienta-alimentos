#!/bin/sh
set -eu

# Official image only creates POSTGRES_USER on an empty data dir. Existing volumes
# may only have role "postgres"; this creates/aligns the Compose app role every start.

docker-entrypoint.sh postgres &
pid=$!
trap 'kill -TERM "$pid" 2>/dev/null; wait "$pid"' TERM INT

until pg_isready -q; do
  sleep 0.3
done

USER_NAME="${POSTGRES_USER:-pimienta_dba}"
PASSWORD="${POSTGRES_PASSWORD:-pimienta_dba}"
DB_NAME="${POSTGRES_DB:-pimienta_alimentos}"

gosu postgres psql -v ON_ERROR_STOP=1 -d postgres \
  --set=user_name="$USER_NAME" \
  --set=pass="$PASSWORD" \
  --set=db_name="$DB_NAME" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'user_name', :'pass')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = :'user_name')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'user_name', :'pass')
WHERE EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = :'user_name')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'user_name')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db_name')
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'db_name', :'user_name')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = :'db_name')
\gexec
SQL

gosu postgres psql -v ON_ERROR_STOP=1 -d "$DB_NAME" \
  --set=user_name="$USER_NAME" <<'SQL'
SELECT format('GRANT ALL ON SCHEMA public TO %I', :'user_name')
\gexec
SELECT format('ALTER SCHEMA public OWNER TO %I', :'user_name')
\gexec
SELECT format('GRANT ALL ON ALL TABLES IN SCHEMA public TO %I', :'user_name')
\gexec
SELECT format('GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO %I', :'user_name')
\gexec

SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'user_name')
FROM pg_tables
WHERE schemaname = 'public'
\gexec

SELECT format('ALTER SEQUENCE public.%I OWNER TO %I', c.relname, :'user_name')
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'S'
\gexec
SQL

wait "$pid"
