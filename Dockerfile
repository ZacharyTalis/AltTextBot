FROM openjdk:21
MAINTAINER glossawy

ARG version

COPY ./ /app
RUN chmod +x /app/entrypoint.sh

SHELL ["/bin/bash", "-c"]

ENV LOG_PATH=/app/logs
ENV DB_PATH=/app/db
ENV BOT_VERSION=${version}

WORKDIR /app

CMD /app/entrypoint.sh
