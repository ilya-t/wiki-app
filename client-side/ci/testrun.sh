set -e
rm -rf ./build_artifacts/
mkdir -p ./build_artifacts

cd client_side_env
./build_image.sh
cd -

set +e
docker run \
    --volume "$(pwd)/build_artifacts:/tmp/build_artifacts" \
    client_side_env:latest \
    /bin/bash -c "./run.sh"
TEST_EXIT=$?
set -e

exit $TEST_EXIT
