set -e
PORT=8181
REPO=$1

if [ "$REPO" == "" ]; then
    REPO="./tests/test_repo"
    echo "WARNING! NO REPO SPECIFIED! USING TEST REPO AT: $REPO"
    cd tests
    ./init_repo.sh
    cd ..
fi

target_ssh_keys=~/.ssh

docker build ./app --tag wiki_backend
docker run \
    --detach \
    --publish $PORT:80 \
    --volume $REPO:/app/repo-store/repo \
    --volume $target_ssh_keys:/root/.ssh \
    --volume /tmp/test_repo.git:/tmp/test_repo.git \
    wiki_backend:latest


echo "server-side is running locally. Check url:"
echo "      http://localhost:$PORT/api/health"
echo "(to forward port to emulator use: adb root && adb forward tcp:$PORT tcp:80)"