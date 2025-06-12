FROM thyrlian/android-sdk:7.1

COPY ./app /app
COPY ./run.sh /app/run.sh
WORKDIR /app

RUN apt-get update && \
    apt-get install -y zip=3.0-11build1 openjdk-21-jdk && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:${PATH}"

# to mount ahead of builds
RUN mkdir -p /app/build/reports
RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
RUN pwd
RUN ls -l
RUN ./gradlew clean
