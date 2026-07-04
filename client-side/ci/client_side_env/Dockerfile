FROM thyrlian/android-sdk:7.1

# docker.io is needed for localrun_for_tests.sh
RUN apt-get update && \
    apt-get install -y zip=3.0-11build1 openjdk-21-jdk docker.io git && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:${PATH}"

WORKDIR /app

# Gradle metadata changes rarely; keep this layer stable across source edits.
COPY ./app/gradlew ./app/gradlew.bat ./app/settings.gradle ./app/build.gradle ./
COPY ./app/gradle/ ./gradle/
COPY ./app/app/build.gradle ./app/app/proguard-rules.pro ./app/app/
COPY ./app/lib-domain/build.gradle ./lib-domain/

RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
RUN mkdir -p /app/build/reports

# Warm ~/.gradle (wrapper distribution + dependency cache) before copying sources.
RUN ./gradlew :lib-domain:dependencies :app:dependencies --no-daemon -q

COPY ./app /app
COPY ./run.sh /app/run.sh

RUN ./gradlew clean --no-daemon -q
