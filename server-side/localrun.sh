set -e
cd tests
./init_repo.sh
cd ..
PORT=8181
echo "host_port=$PORT" > .env
docker-compose up --build --detach
echo "server-side is running locally. Check url:"
echo "      http://localhost:$PORT/api/health"
echo "(to forward port to emulator use: adb root && adb forward tcp:$PORT tcp:80)"