set +e

rm -rf /tmp/build_artifacts
mkrdir -p /tmp/build_artifacts

./gradlew test #lint
TEST_EXIT=$?

set -e

cp -R app/build/reports /tmp/build_artifacts/app
cp -R lib-domain/build/reports /tmp/build_artifacts/lib-domain
exit $TEST_EXIT
