set -e
rm -rf ./build_artifacts/
mkdir -p ./build_artifacts

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

# SyncTests bind-mount paths are resolved on the CI runner host when using the
# Docker socket, so test data must live on a host directory mounted at the same
# path inside the client-side container (CI_TEST_ENV_DIR).
HOST_TEST_TMP=$(mktemp -d)
# Nested wiki_backend containers create root-owned files; clean up via Docker.
#cleanup() {
#    if [ -n "$HOST_TEST_TMP" ] && [ -d "$HOST_TEST_TMP" ]; then
#        docker run --rm -v "$HOST_TEST_TMP:$HOST_TEST_TMP" alpine:3.15 rm -rf "$HOST_TEST_TMP" 2>/dev/null || true
#    fi
#}
#trap cleanup EXIT

cd client_side_env
./build_image.sh
cd -

# Pre-build on the runner host so SyncTests do not spend minutes rebuilding
# inside each test and exceed the 120s heartbeat timeout.
echo "Pre-building server image for integration tests..."
docker build "$REPO_ROOT/server-side/app" --tag server-side-app:latest

set +e
DOCKER_GID=$(stat -c '%g' /var/run/docker.sock)
# Host network: nested wiki_backend publishes 8181 on the runner, not inside this container.
docker run \
    --network host \
    --group-add "$DOCKER_GID" \
    --volume "$(pwd)/build_artifacts:/tmp/build_artifacts" \
    --volume "$REPO_ROOT/server-side:/server-side:ro" \
    --volume "$HOST_TEST_TMP:$HOST_TEST_TMP" \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    -e CI_TEST_ENV_DIR="$HOST_TEST_TMP" \
    -e SKIP_BUILD_IMAGE=1 \
    client_side_env:latest \
    /bin/bash -c "./run.sh"
TEST_EXIT=$?
set -e

exit $TEST_EXIT
