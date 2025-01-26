set -e
cd app
docker build --tag=server-side-app .
cd -
