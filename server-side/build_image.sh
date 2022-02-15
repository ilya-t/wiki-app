set -e
cd app
docker build --progress=plain --tag=server-side-app .
cd -
