set -e
# USAGE: specify link to remote repo as first arg. For example: ./localrun.sh git@github.com:ilya-t/wiki-app.git
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

# if [[ -d "$REPO" ]]; then
#     # TODO: mount bare local repo for cloning?
# fi

docker build ./app --tag wiki_backend
docker container rm --force wiki_backend_local

docker run \
    --detach \
    --publish $PORT:80 \
    --env APP_REPO_LINK=$REPO \
    --volume $target_ssh_keys:/root/.ssh \
    --name wiki_backend_local \
    wiki_backend:latest


echo "server-side is running locally. Check url:"
echo "      http://localhost:$PORT/api/health"
echo "(to forward port to emulator use: adb root && adb forward tcp:$PORT tcp:80)"