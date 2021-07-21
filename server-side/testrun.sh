set -e
rm -rf ./build_artifacts
mkdir ./build_artifacts

cd tests
./init_repo.sh
cd ./test_repo
commit=$(git rev-parse HEAD~0)
cd ../..
set +e
echo "head_commit=$commit" > .env
echo "host_port=80" >> .env
echo "target_repo=./tests/test_repo" >> .env
docker-compose up --build --abort-on-container-exit
RESULT=$?
docker-compose logs > ./build_artifacts/compose.log
echo "Tests report ./build_artifacts/report.html"
exit $RESULT
