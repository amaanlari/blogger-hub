#!/usr/bin/env bash
#
# Container entrypoint. Every backing service (MongoDB Atlas, Aiven Kafka, Redis) is external, so
# there is nothing to orchestrate here — this just assembles the app's configuration sources and
# hands the container over to the JVM.
#
# Configuration can arrive two ways, and they compose:
#   1. Render Secret File  /etc/secrets/application-staging.yaml  (all credentials in one upload)
#   2. Plain environment variables                                (override anything in the file)

set -euo pipefail

log() { echo "[start.sh] $*"; }

APP_ARGS=()

# Render mounts Secret Files at /etc/secrets/<filename>. Using one keeps credentials out of the
# image, out of git, and out of the dashboard's env var list. An `additional-location` is layered
# on top of the JAR's own application.yaml, and real env vars still win over both.
SECRETS_CONFIG="${SPRING_SECRETS_CONFIG:-/etc/secrets/application-staging.yaml}"
if [ -f "${SECRETS_CONFIG}" ]; then
  log "Loading additional configuration from ${SECRETS_CONFIG}"
  APP_ARGS+=("--spring.config.additional-location=file:${SECRETS_CONFIG}")
else
  log "No secret file at ${SECRETS_CONFIG}; expecting configuration from environment variables."
fi

# The Aiven CA certificate the Kafka client validates the broker against. It is a public
# certificate, not a credential, but it lives beside the secret file so it is uploaded the same
# way. Fail fast with a clear message rather than letting the Kafka client retry a TLS handshake
# forever behind an opaque "Failed to load SSL keystore" in the logs.
KAFKA_SSL_TRUSTSTORE_PATH="${KAFKA_SSL_TRUSTSTORE_PATH:-/etc/secrets/ca.pem}"
export KAFKA_SSL_TRUSTSTORE_PATH
if [ ! -r "${KAFKA_SSL_TRUSTSTORE_PATH}" ]; then
  log "FATAL: Kafka CA certificate not readable at ${KAFKA_SSL_TRUSTSTORE_PATH}."
  log "Add it as a Render Secret File named 'ca.pem' (Aiven console -> service -> CA certificate),"
  log "or point KAFKA_SSL_TRUSTSTORE_PATH somewhere else."
  exit 1
fi

log "Starting Blogger Hub (profile: ${SPRING_PROFILES_ACTIVE:-default}) on port ${PORT:-8080}..."

# exec so the JVM becomes PID 1 and receives Render's SIGTERM directly, which lets Spring Boot run
# its graceful shutdown instead of being killed by the 30s force-kill timer.
# shellcheck disable=SC2086 # JAVA_OPTS is intentionally word-split into separate JVM flags.
exec java ${JAVA_OPTS:-} -jar /app/app.jar "${APP_ARGS[@]}"
