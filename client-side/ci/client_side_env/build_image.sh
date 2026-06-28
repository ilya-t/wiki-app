set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_SIDE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

rm -rf "$SCRIPT_DIR/app"
# Stage client-side in a temp dir first: copying directly into client_side_env/app
# would recurse into the destination (client-side/ci/client_side_env/app).
BUILD_DIR=$(mktemp -d)
trap 'rm -rf "$BUILD_DIR"' EXIT
cp -R "$CLIENT_SIDE_DIR" "$BUILD_DIR/app"
mv "$BUILD_DIR/app" "$SCRIPT_DIR/app"

docker build "$SCRIPT_DIR" --progress=plain --tag client_side_env:latest
rm -rf "$SCRIPT_DIR/app"
