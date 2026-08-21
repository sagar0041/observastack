#!/bin/sh
# Creates the second, separate database inventory-service owns.
#
# The official postgres image runs every script in
# /docker-entrypoint-initdb.d/ once, only when the data directory is
# empty (first-ever container start with a fresh volume) — it does NOT
# rerun on every restart. If you already ran `docker compose up` before
# M3, your existing postgres-data volume was initialized without this
# script; recreate it (`docker compose down -v` then `up -d` — this
# drops all local Postgres data) or run the CREATE DATABASE below by
# hand against the running container.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
    CREATE DATABASE inventory OWNER $POSTGRES_USER;
SQL
