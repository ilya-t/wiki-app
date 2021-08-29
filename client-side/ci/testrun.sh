set -e
rm -rf ./build_artifacts/
cd client_side_env
./build_image.sh
cd -

docker run \
    --volume $(pwd)/build_artifacts:/tmp/build_artifacts \
    client_side_env:latest
    /app/run.sh
