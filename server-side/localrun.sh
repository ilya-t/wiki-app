set -e
cd tests
./init_repo.sh
cd ..
docker-compose up --build --detach
echo "server-side is running locally. Check url:"
echo "      http://localhost/api/health"