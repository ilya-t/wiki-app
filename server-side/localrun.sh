set -e
cd tests
./init_repo.sh
cd ..
docker-compose up --build --detach
echo "server-side is running locally. Check url:"
echo "      http://localhost/api/health"
echo "(to forward port to emulator use: adb root && adb forward tcp:80 tcp:80)"