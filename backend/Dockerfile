# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
# Optional outbound proxy for restricted networks (Docker Desktop: http://http.docker.internal:3128).
# Maven ignores HTTP_PROXY/HTTPS_PROXY, so the value is translated into ~/.m2/settings.xml.
ARG HTTP_PROXY=""
ARG HTTPS_PROXY=""
ENV HTTP_PROXY=${HTTP_PROXY} HTTPS_PROXY=${HTTPS_PROXY}
RUN set -eu; \
    proxy="${HTTPS_PROXY:-$HTTP_PROXY}"; \
    if [ -n "$proxy" ]; then \
        hp="${proxy#*://}"; hp="${hp%%/*}"; \
        host="${hp%%:*}"; port="${hp##*:}"; \
        if [ "$port" = "$host" ]; then port=80; fi; \
        mkdir -p /root/.m2; \
        printf '<settings><proxies><proxy><id>build-https</id><active>true</active><protocol>https</protocol><host>%s</host><port>%s</port></proxy><proxy><id>build-http</id><active>true</active><protocol>http</protocol><host>%s</host><port>%s</port></proxy></proxies></settings>\n' \
            "$host" "$port" "$host" "$port" > /root/.m2/settings.xml; \
    fi
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel
COPY --from=build /app/target/*.jar app.jar
USER sentinel
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
