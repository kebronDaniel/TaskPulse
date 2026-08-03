FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system taskpulse \
    && useradd --system \
        --gid taskpulse \
        --home-dir /app \
        --shell /usr/sbin/nologin \
        taskpulse

COPY --from=builder --chown=taskpulse:taskpulse \
     /workspace/target/TaskPulse-*.jar app.jar

USER taskpulse

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]