set -e
CLIENT_SIDE_DIR=../..
rm -rf ./app
rm -rf /tmp/app_shapshot
mkdir /tmp/app_shapshot

cp -R $CLIENT_SIDE_DIR /tmp/app_shapshot/
mv /tmp/app_shapshot ./app

docker build . --progress=plain --tag client_side_env:latest
rm -rf ./app