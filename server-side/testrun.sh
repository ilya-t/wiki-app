set -e
echo "Checking compose availability"
docker compose version

repo_on_app=/tmp/server_side_test_volumes/repo-store
config_dir=/tmp/server_side_test_volumes/config
rm -rf $repo_on_app
mkdir -p $repo_on_app

rm -rf $config_dir
mkdir -p $config_dir

rm -rf ./build_artifacts
mkdir ./build_artifacts

echo "Preparing test repo"
cd tests
./init_repo.sh
cp ./test_config.json $config_dir/config.json
cd ..

echo "Running integration tests"
set +e
rm .env
echo "host_port=80" >> .env
echo "repo_store_dir=$repo_on_app" >> .env
echo "config_dir=$config_dir" >> .env
echo "target_ssh_keys=~/.ssh" >> .env
docker compose up --build --abort-on-container-exit
RESULT_INTGR=$?
docker compose logs > ./build_artifacts/compose.log
docker compose down --volumes

echo "Building images"
./build_image.sh

echo "Running unit tests"
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
echo "Full Log:          ./build_artifacts/compose.log"


if [ $RESULT_INTGR != "0" ] ; then
    exit 1
fi

if [ $RESULT_UNIT != "0" ] ; then
    exit 1
fi

echo "  All tests passed!"