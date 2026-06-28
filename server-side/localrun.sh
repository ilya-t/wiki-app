set -e
PORT=8181
host_volumes=$1
DETACH_ARG=$2
ALIAS=$3
CONTAINER_NAME="${CONTAINER_NAME:-wiki_backend_local}"
WAIT_TIMEOUT_SEC="${WAIT_TIMEOUT_SEC:-120}"

if [ "$host_volumes" == "" ]; then
    echo "Specify path to volumes as first arg: ./localrun.sh /some/path "
    echo "Expecting stucture at /some/path:"
    echo "  /some/path/config/config.json"
    echo "  /some/path/repo-store"
    exit 1
fi

wait_for_previous_container() {
    remaining=$WAIT_TIMEOUT_SEC

    while docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; do
        if [ "$remaining" -le 0 ]; then
            echo "--> Timeout waiting for $CONTAINER_NAME to finish after ${WAIT_TIMEOUT_SEC}s"
            exit 1
        fi

        running=$(docker container inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null || echo false)
        if [ "$running" = "true" ]; then
            echo "--> Previous $CONTAINER_NAME is still running, waiting for it to finish... (${remaining}s left)"
        else
            echo "--> Removing stopped $CONTAINER_NAME"
            docker container rm --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
            return 0
        fi

        sleep 1
        remaining=$((remaining - 1))
    done
}

host_ssh_keys=~/.ssh

mkdir -p $host_volumes/config
mkdir -p $host_volumes/repo-store

wait_for_previous_container

if [ "$SKIP_BUILD_IMAGE" != "1" ]; then
    ./build_image.sh
fi

RESTART_POLICY="${RESTART_POLICY:-unless-stopped}"
docker run \
    $( [ "$DETACH_ARG" == "--no-detach" ] || echo "--detach" ) \
    --publish $PORT:80 \
    --env APP_REPO_LINK=$REPO \
    --env ALIAS="$ALIAS" \
    --volume $host_ssh_keys:/root/.ssh \
    --volume $host_volumes/config:/app/config \
    --volume $host_volumes/repo-store:/app/repo-store \
    --volume $host_volumes/remote-repos:/app/remote-repos \
    --restart "$RESTART_POLICY" \
    --name "$CONTAINER_NAME" \
    server-side-app:latest


echo "server-side is running locally. Check url:"
echo "      http://localhost:$PORT/api/health"
