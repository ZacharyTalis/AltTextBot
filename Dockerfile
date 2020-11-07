FROM openjdk:14-alpine
MAINTAINER glossawy

ARG version

COPY ./ /app
RUN apk add bash
RUN chmod +x /app/entrypoint.sh

SHELL ["/bin/bash", "-c"]

ENV LOG_PATH=/app/logs
ENV DB_PATH=/app/db
ENV BOT_VERSION=${version}

CMD /app/entrypoint.sh
