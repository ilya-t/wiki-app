set -e
rm -rf ./build_artifacts
mkdir ./build_artifacts
set +e
docker-compose up --build --abort-on-container-exit
RESULT=$?
docker-compose logs > ./build_artifacts/compose.log
echo "Tests report ./build_artifacts/report.html"
exit $RESULT
