FROM python:3.6-alpine3.12
RUN pip3 install requests
RUN pip3 install pytest-html==1.22.0
RUN apk add --no-cache tree
COPY ./app /app/
RUN chmod +x /app/run.sh
WORKDIR /app
CMD ./run.sh