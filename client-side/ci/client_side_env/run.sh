#!/bin/bash
set +e

mkdir -p /tmp/build_artifacts
rm -rf /tmp/build_artifacts/*

./gradlew test :app:assembleDebug > /tmp/build_artifacts/gradle.log 2>&1
TEST_EXIT=$?

if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    cp app/build/outputs/apk/debug/app-debug.apk /tmp/build_artifacts/
fi
if [ -d app/build/reports ]; then
    cp -R app/build/reports /tmp/build_artifacts/app
fi
if [ -d lib-domain/build/reports ]; then
    cp -R lib-domain/build/reports /tmp/build_artifacts/lib-domain
fi

exit $TEST_EXIT
