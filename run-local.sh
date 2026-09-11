#!/usr/bin/env bash
#
# Blogger Hub — one-command local run script.
#
# Starts MongoDB, Redis, and Kafka via the existing docker-compose.yaml, works around two real
# bugs discovered in that setup (the kafka-init topic-creation script doesn't survive Compose's
# parsing, and KafkaConfig hardcodes SSL against what is normally a plaintext local broker),
# then builds and runs the app (backend + the bundled React frontend, in one JAR).
#
# Safe to re-run: the docker containers and the Kafka topic are created idempotently, and JWT
# secrets are generated once and persisted locally so restarting doesn't invalidate every
# previously-issued session.
#
# Usage:
#   ./run-local.sh                 build (backend + frontend) and run
#   ./run-local.sh --skip-build    run the existing target/*.jar without rebuilding
#   ./run-local.sh --dev           run the backend via `mvn spring-boot:run` and the frontend
#                                   via `npm run dev` (hot reload for both); Ctrl+C stops both
#   ./run-local.sh --stop          stop and remove the local infra containers
#   ./run-local.sh --help          show this message
#
# Every variable below only gets a default value if you haven't already exported one yourself —
# export real Cloudinary/Gmail/mail credentials before running this script to use them instead
# of the safe dummy values (uploads/emails silently no-op with the dummy ones, everything else
# works normally).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

STATE_DIR="/tmp/blogger-hub-local"
SECRETS_FILE="$STATE_DIR/secrets.env"
COMPOSE_INFRA_SERVICES=(mongodb redis kafka kafka-init)
NOTIFICATIONS_TOPIC="blogger-hub-notifications"

log() { printf '\n==> %s\n' "$*"; }

SKIP_BUILD=false
DEV_MODE=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --dev) DEV_MODE=true ;;
    --stop)
      log "Stopping local infra containers (mongodb/redis/kafka/kafka-init)..."
      docker compose down
      exit 0
      ;;
    -h|--help)
      sed -n '2,29p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "Unknown option: $arg (use --help)" >&2
      exit 1
      ;;
  esac
done

command -v docker >/dev/null 2>&1 || { echo "docker is required but not found on PATH." >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "Java 21 is required but not found on PATH." >&2; exit 1; }

mkdir -p "$STATE_DIR"

# --- 1. Infra -----------------------------------------------------------------------------

log "Starting MongoDB, Redis, and Kafka (docker compose)..."
docker compose up -d "${COMPOSE_INFRA_SERVICES[@]}"

log "Waiting for Kafka to report healthy..."
for _ in $(seq 1 30); do
  status="$(docker inspect --format '{{.State.Health.Status}}' kafka 2>/dev/null || echo unknown)"
  [[ "$status" == "healthy" ]] && break
  sleep 2
done
if [[ "$status" != "healthy" ]]; then
  echo "Kafka did not report healthy in time — check 'docker compose logs kafka'." >&2
  exit 1
fi

log "Ensuring the '$NOTIFICATIONS_TOPIC' topic exists (kafka-init's own script doesn't reliably create it — see docs/FRONTEND.md)..."
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 \
  --create --if-not-exists --topic "$NOTIFICATIONS_TOPIC" \
  --partitions 3 --replication-factor 1 >/dev/null

# --- 2. Throwaway Kafka SSL keystore/truststore --------------------------------------------
# KafkaConfig.java hardcodes security.protocol=SSL for both producer and consumer, regardless
# of the broker's actual protocol. The local Kafka container above runs PLAINTEXT, so real
# message delivery will still fail in the background even with this in place — this step exists
# only so the app doesn't crash at startup (the @KafkaListener consumer eagerly parses these
# files and kills the whole app if they're missing or structurally invalid).

if [[ ! -f "$STATE_DIR/kafka-keystore.pem" ]]; then
  log "Generating a throwaway self-signed cert for the Kafka SSL client config..."
  openssl req -x509 -newkey rsa:2048 -nodes \
    -keyout "$STATE_DIR/kafka-key.pem" -out "$STATE_DIR/kafka-cert.pem" \
    -days 3650 -subj "/CN=blogger-hub-local" >/dev/null 2>&1
  cat "$STATE_DIR/kafka-key.pem" "$STATE_DIR/kafka-cert.pem" > "$STATE_DIR/kafka-keystore.pem"
  cp "$STATE_DIR/kafka-cert.pem" "$STATE_DIR/kafka-truststore.pem"
fi

# --- 3. Env vars ----------------------------------------------------------------------------
# JWT secrets are generated once and persisted so restarting this script doesn't invalidate
# every previously-issued access/refresh token.

if [[ ! -f "$SECRETS_FILE" ]]; then
  log "Generating persistent local JWT secrets ($SECRETS_FILE)..."
  {
    echo "export ACCESS_TOKEN_SECRET=$(openssl rand -hex 32)"
    echo "export REFRESH_TOKEN_SECRET=$(openssl rand -hex 32)"
  } > "$SECRETS_FILE"
fi
# shellcheck disable=SC1090
source "$SECRETS_FILE"

: "${MONGODB_URI:=mongodb://localhost:27018/blogger_hub_db}"
: "${REDIS_HOST:=localhost}"
: "${REDIS_PORT:=6380}"
: "${REDIS_USERNAME:=}"
: "${REDIS_PASSWORD:=}"
: "${ACCESS_TOKEN_EXPIRATION_MINUTES:=15}"
: "${REFRESH_TOKEN_EXPIRATION_DAYS:=7}"
: "${OTP_TTL:=120}"
: "${CLOUDINARY_CLOUD_NAME:=test-cloud}"
: "${CLOUDINARY_API_KEY:=123456789012345}"
: "${CLOUDINARY_API_SECRET:=dummysecret}"
: "${CLOUDINARY_DEFAULT_PROFILE_PIC:=https://example.com/default.png}"
: "${MAIL_HOST:=localhost}"
: "${MAIL_PORT:=2525}"
: "${MAIL_USERNAME:=test@example.com}"
: "${MAIL_PASSWORD:=dummy}"
: "${GMAIL_CLIENT_ID:=dummy-client-id}"
: "${GMAIL_CLIENT_SECRET:=dummy-client-secret}"
: "${GMAIL_REFRESH_TOKEN:=dummy-refresh-token}"
: "${AIVEN_BASIC_AUTH_USER_INFO:=dummy:dummy}"
: "${KAFKA_BOOTSTRAP_SERVERS:=localhost:9092}"
: "${KAFKA_SSL_TRUSTSTORE_PATH:=$STATE_DIR/kafka-truststore.pem}"
: "${KAFKA_SSL_KEYSTORE_PATH:=$STATE_DIR/kafka-keystore.pem}"

export MONGODB_URI REDIS_HOST REDIS_PORT REDIS_USERNAME REDIS_PASSWORD \
       ACCESS_TOKEN_SECRET REFRESH_TOKEN_SECRET \
       ACCESS_TOKEN_EXPIRATION_MINUTES REFRESH_TOKEN_EXPIRATION_DAYS OTP_TTL \
       CLOUDINARY_CLOUD_NAME CLOUDINARY_API_KEY CLOUDINARY_API_SECRET CLOUDINARY_DEFAULT_PROFILE_PIC \
       MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD \
       GMAIL_CLIENT_ID GMAIL_CLIENT_SECRET GMAIL_REFRESH_TOKEN AIVEN_BASIC_AUTH_USER_INFO \
       KAFKA_BOOTSTRAP_SERVERS KAFKA_SSL_TRUSTSTORE_PATH KAFKA_SSL_KEYSTORE_PATH

# --- 4. Run -----------------------------------------------------------------------------------

VITE_PID=""
cleanup() {
  if [[ -n "$VITE_PID" ]] && kill -0 "$VITE_PID" 2>/dev/null; then
    log "Stopping frontend dev server..."
    kill "$VITE_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

if $DEV_MODE; then
  if [[ ! -d frontend/node_modules ]]; then
    log "Installing frontend dependencies (first run)..."
    (cd frontend && npm install)
  fi
  log "Starting frontend dev server in the background (http://localhost:5173)..."
  (cd frontend && npm run dev) &
  VITE_PID=$!

  log "Starting backend (mvn spring-boot:run) — http://localhost:8080. Ctrl+C stops both."
  ./mvnw spring-boot:run
else
  if ! $SKIP_BUILD; then
    log "Building the app (backend + React frontend bundled into one JAR)..."
    ./mvnw package -DskipTests
  fi
  jar_file="$(ls target/blogger-hub-*.jar 2>/dev/null | grep -v sources | head -1)"
  if [[ -z "$jar_file" ]]; then
    echo "No jar found in target/ — remove --skip-build or run './mvnw package' first." >&2
    exit 1
  fi
  log "Starting $jar_file — open http://localhost:8080 once it's up. Ctrl+C stops it."
  java -jar "$jar_file"
fi
