FROM openjdk:16-alpine
MAINTAINER glossawy (glossawy@protonmail.com)

ARG token
ARG adminpass

ENV BOT_TOKEN=${token}
ENV ADMIN_APP_PASS=${adminpass}
ENV LOG_PATH=/app/logs

COPY . /app
VOLUME /app/logs
WORKDIR /app

CMD ["java", "-jar", "/app/out/artifacts/javacord_jar/javacord.jar"]
