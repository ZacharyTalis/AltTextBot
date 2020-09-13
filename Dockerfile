FROM openjdk:14-alpine
MAINTAINER glossawy

ARG token
ARG adminpass
ARG adminuser

ENV BOT_TOKEN=${token}
ENV ADMIN_APP_PASS=${adminpass}
ENV ADMIN_APP_USER=${adminuser}
ENV LOG_PATH=/app/logs

COPY . /app
VOLUME /app/logs
WORKDIR /app

CMD ["java", "--enable-preview", "-jar", "/app/build/libs/AltTextBot-1.0.jar"]
