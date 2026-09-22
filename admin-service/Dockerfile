FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

RUN addgroup -S application \
    && adduser -S application -G application

COPY --chown=application:application admin-service/target/study-admin-service-*.jar application.jar

USER application

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
