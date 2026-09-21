#!/bin/sh
# Starts Postgres (via Docker Compose) and runs the backend, which seeds
# demo data on first boot. Assumes Docker is already running.
set -e

cd "$(git rev-parse --show-toplevel)"

echo "Starting Postgres..."
docker compose up -d

echo "Waiting for Postgres to be ready..."
until docker compose exec -T postgres pg_isready -U handoff -d handoff >/dev/null 2>&1; do
  sleep 1
done
echo "Postgres is ready."

cd backend
./mvnw spring-boot:run
