FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

RUN addgroup -S application \\
    && adduser -S application -G application

COPY --chown=application:application image-detect-service/target/image-detect-service-*.jar application.jar

USER application

EXPOSE 50052

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
