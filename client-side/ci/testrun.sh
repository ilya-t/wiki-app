set -e
rm -rf ./build_artifacts/
cd client_side_env
./build_image.sh
cd -

docker run \
    --volume $(pwd)/build_artifacts:/app/app/build/reports \
    client_side_env:latest \
    ./gradlew test #lint
