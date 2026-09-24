FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

RUN addgroup -S application \\
    && adduser -S application -G application

COPY --chown=application:application audio-detect-service/target/audio-detect-service-*.jar application.jar

USER application

EXPOSE 50053

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
