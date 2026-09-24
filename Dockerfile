FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

RUN addgroup -S application \
    && adduser -S application -G application

COPY --chown=application:application access-service/target/study-access-service-*.jar application.jar

USER application

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
