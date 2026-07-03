# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="bankcards" \
      org.opencontainers.image.description="Bank Cards Management REST API" \
      org.opencontainers.image.source="https://github.com/charset-8utf/bank-rest"

WORKDIR /app

RUN apk add --no-cache wget tini \
 && apk upgrade --no-cache \
 && addgroup -S -g 1000 appgroup \
 && adduser -S -u 1000 -G appgroup appuser \
 && chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /build/target/bankcards-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
