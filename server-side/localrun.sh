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

echo "host_port=$PORT" > .env
echo "target_repo=$REPO" >> .env
docker-compose up --build --detach
echo "server-side is running locally. Check url:"
echo "      http://localhost:$PORT/api/health"
echo "(to forward port to emulator use: adb root && adb forward tcp:$PORT tcp:80)"