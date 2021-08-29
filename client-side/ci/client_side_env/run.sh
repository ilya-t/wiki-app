set -e
./gradlew test #lint
cp -R app/build/reports /tmp/build_artifacts/app
cp -R lib-domain/build/reports /tmp/build_artifacts/lib-domain
