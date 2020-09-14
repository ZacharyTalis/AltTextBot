FROM openjdk:14-alpine
MAINTAINER glossawy

COPY ./build/libs /app
RUN apk add bash
RUN chmod +x /app/entrypoint.sh

SHELL ["/bin/bash", "-c"]

ENV LOG_PATH=/app/logs
VOLUME /app/logs

CMD /app/entrypoint.sh
