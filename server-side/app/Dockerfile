# FROM arm64v8/golang:1.14.1-buster
FROM golang:1.17.7-alpine3.15
EXPOSE 80
# git verion not specified cause https://gitlab.alpinelinux.org/alpine/abuild/-/issues/9996
RUN apk add --no-cache git
RUN apk add --no-cache zip
RUN apk add --no-cache openssh
RUN apk add --no-cache alpine-sdk

RUN go get github.com/stretchr/testify/assert

RUN mkdir /app

RUN mkdir -p /app/repo-store/repo
COPY ./repo-store /app/repo-store
COPY ./src /app/src

WORKDIR /app/src
RUN go build -o /app/backend_app
CMD /app/backend_app
