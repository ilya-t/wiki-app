FROM thyrlian/android-sdk:6.0

COPY ./app /app
WORKDIR /app

# to mount ahead of builds
RUN mkdir -p /app/build/reports
RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
RUN pwd
RUN ls -l
RUN ./gradlew clean
