set -e
rm -rf ./build_artifacts
mkdir ./build_artifacts

cd tests
./init_repo.sh
cd ..
set +e
echo "host_port=80" >> .env
echo "target_repo=./tests/test_repo" >> .env
echo "target_ssh_keys=~/.ssh" >> .env
docker-compose up --build --abort-on-container-exit
RESULT=$?
docker-compose logs > ./build_artifacts/compose.log
echo "Tests report ./build_artifacts/report.html"
exit $RESULT
