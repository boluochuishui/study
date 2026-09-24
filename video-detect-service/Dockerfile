FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

RUN addgroup -S application \\
    && adduser -S application -G application

COPY --chown=application:application video-detect-service/target/video-detect-service-*.jar application.jar

USER application

EXPOSE 50054

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
