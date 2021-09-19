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
RESULT=$?
docker-compose logs > ./build_artifacts/compose.log
docker-compose down --volumes
echo "Tests report ./build_artifacts/report.html"
exit $RESULT
