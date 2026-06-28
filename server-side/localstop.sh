#!/bin/sh
set -e

CONTAINER_NAME="${CONTAINER_NAME:-wiki_backend_local}"
WAIT_TIMEOUT_SEC="${WAIT_TIMEOUT_SEC:-60}"

deadline=$(( $(date +%s) + WAIT_TIMEOUT_SEC ))

while docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; do
    running=$(docker container inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null || echo false)
    if [ "$running" = "true" ]; then
        echo "--> Stopping $CONTAINER_NAME"
        docker stop -t 5 "$CONTAINER_NAME" >/dev/null 2>&1 || true
    else
        echo "--> Removing stopped $CONTAINER_NAME"
        docker container rm --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
        exit 0
    fi

    if [ "$(date +%s)" -ge "$deadline" ]; then
        echo "--> Timeout waiting for $CONTAINER_NAME to stop, forcing removal"
        docker container rm --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
        exit 0
    fi

    sleep 0.5
done
