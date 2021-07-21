FROM thyrlian/android-sdk:6.0

COPY ./app /app
WORKDIR /app

RUN apt-get update && apt-get install zip=3.0-11build1

# to mount ahead of builds
RUN mkdir -p /app/build/reports
RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
RUN pwd
RUN ls -l
RUN ./gradlew clean
