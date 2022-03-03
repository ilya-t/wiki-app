set -e
PORT=8181
host_volumes=$1

if [ "$host_volumes" == "" ]; then
    echo "Specify path to volumes as first arg: ./localrun.sh /some/path "
    echo "Expecting stucture at /some/path:"
    echo "  /some/path/config/config.json"
    echo "  /some/path/repo-store"
    exit 1
fi

host_ssh_keys=~/.ssh

mkdir -p $host_volumes/config
mkdir -p $host_volumes/repo-store

docker build ./app --tag wiki_backend
docker container rm --force wiki_backend_local

docker run \
    --detach \
    --publish $PORT:80 \
    --env APP_REPO_LINK=$REPO \
    --volume $host_ssh_keys:/root/.ssh \
    --volume $host_volumes/config:/app/config \
    --volume $host_volumes/repo-store:/app/repo-store \
    --name wiki_backend_local \
    wiki_backend:latest


echo "server-side is running locally. Check url:"
echo "      http://localhost:$PORT/api/health"
echo "(to forward port to emulator use: adb root && adb forward tcp:$PORT tcp:80)"