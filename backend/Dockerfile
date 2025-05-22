FROM gradle:8.8-jdk21-alpine AS build

ENV GRADLE_USER_HOME=/gradle
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

COPY src /app/src

ARG RUN_TESTS=false
RUN if [ "$RUN_TESTS" = "true" ]; then \
      gradle clean build --no-daemon -x pitest; \
    else \
      gradle clean bootJar --no-daemon -x test -x pitest; \
    fi

FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL org.opencontainers.image.source="https://github.com/sorinGrecu/sgrecu-home-assignment" \
      maintainer="Sorin Grecu <sorin@grecu.nl>" \
      org.opencontainers.image.description="Chat service – Spring Boot Backend"

RUN addgroup -S spring && adduser -S spring -G spring && apk add --no-cache wget
USER spring:spring

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]