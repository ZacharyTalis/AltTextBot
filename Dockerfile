FROM openjdk:14-alpine
MAINTAINER glossawy

ARG token
ARG adminpass
ARG adminuser

ENV BOT_TOKEN=${token}
ENV ADMIN_APP_PASS=${adminpass}
ENV ADMIN_APP_USER=${adminuser}
ENV LOG_PATH=/app/logs

VOLUME /app/logs

COPY ./build/libs /app

WORKDIR /app
CMD ["java", "--enable-preview", "-jar", "/app/AltTextBot-1.0.jar"]
