set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_SIDE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

rm -rf "$SCRIPT_DIR/app"
cp -R "$CLIENT_SIDE_DIR" "$SCRIPT_DIR/app"

docker build "$SCRIPT_DIR" --progress=plain --tag client_side_env:latest
rm -rf "$SCRIPT_DIR/app"
