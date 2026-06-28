#!/bin/bash
set +e

mkdir -p /tmp/build_artifacts
rm -rf /tmp/build_artifacts/*

./gradlew test > /tmp/build_artifacts/gradle.log 2>&1
TEST_EXIT=$?

if [ -d app/build/reports ]; then
    cp -R app/build/reports /tmp/build_artifacts/app
fi
if [ -d lib-domain/build/reports ]; then
    cp -R lib-domain/build/reports /tmp/build_artifacts/lib-domain
fi

exit $TEST_EXIT
