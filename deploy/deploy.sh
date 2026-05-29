#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/eyeon-be}"
BRANCH="${BRANCH:-main}"
SERVICE_NAME="${SERVICE_NAME:-eyeon-be.service}"

echo "[deploy] app_dir=${APP_DIR}, branch=${BRANCH}, service=${SERVICE_NAME}"

cd "${APP_DIR}"

if [[ ! -f ".env" ]]; then
  echo "[deploy] ERROR: ${APP_DIR}/.env not found"
  exit 1
fi

echo "[deploy] 1) Pull latest source"
git fetch origin "${BRANCH}"
git checkout "${BRANCH}"
git pull --ff-only origin "${BRANCH}"

echo "[deploy] 2) Ensure infra containers are up (mysql, redis)"
if docker compose version >/dev/null 2>&1; then
  docker compose up -d mysql redis
elif command -v docker-compose >/dev/null 2>&1; then
  docker-compose up -d mysql redis
else
  echo "[deploy] ERROR: neither 'docker compose' nor 'docker-compose' is available"
  exit 1
fi

echo "[deploy] 3) Build bootJar"
./gradlew clean bootJar --no-daemon

echo "[deploy] 4) Restart service"
sudo systemctl restart "${SERVICE_NAME}"
sudo systemctl is-active --quiet "${SERVICE_NAME}"

echo "[deploy] 5) Tail recent logs"
sudo journalctl -u "${SERVICE_NAME}" -n 60 --no-pager

echo "[deploy] Done"
