FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

RUN addgroup -S application \\
    && adduser -S application -G application

COPY --chown=application:application text-detect-service/target/text-detect-service-*.jar application.jar

USER application

EXPOSE 50051

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
