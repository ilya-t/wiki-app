# FROM arm64v8/golang:1.14.1-buster
FROM golang:1.14.1-alpine3.11
EXPOSE 80
RUN apk add --no-cache git=2.24.4-r0

RUN mkdir /app

RUN mkdir -p /app/repo-store/repo
COPY ./repo-store /app/repo-store
COPY ./src /app/src

WORKDIR /app/src
RUN go build -o /app/backend_app
CMD /app/backend_app
