set -e
repo_on_app=/tmp/test_repo_folder
rm -rf $repo_on_app
mkdir -p $repo_on_app
rm -rf ./build_artifacts
mkdir ./build_artifacts

cd tests
./init_repo.sh
cd ..
set +e
rm .env
echo "host_port=80" >> .env
echo "target_repo=$repo_on_app" >> .env
echo "target_ssh_keys=~/.ssh" >> .env
docker-compose up --build --abort-on-container-exit
RESULT_INTGR=$?
docker-compose logs > ./build_artifacts/compose.log
docker-compose down --volumes

echo "Running unit tests"
./build_image.sh
docker run --rm \
--name server-side-units \
server-side-app:latest \
sh -c "go test -timeout 30s"  > ./build_artifacts/unit_test.log
RESULT_UNIT=$?

STATUS_I=""
STATUS_U=""

if [ $RESULT_INTGR != "0" ] ; then
    STATUS_I="(FAILED)"
fi

if [ $RESULT_UNIT != "0" ] ; then
    STATUS_U="(FAILED)"
fi

echo "Integration tests: ./build_artifacts/report.html $STATUS_I"
echo "Unit tests:        ./build_artifacts/unit_test.log $STATUS_U"


if [ $RESULT_INTGR != "0" ] ; then
    exit 1
fi

if [ $RESULT_UNIT != "0" ] ; then
    exit 1
fi

echo "  All tests passed!"