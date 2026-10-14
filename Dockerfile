FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/data && chown -R 10001:0 /app
COPY --from=build --chown=10001:0 /workspace/target/support-ticket-automation-0.0.1-SNAPSHOT.jar /app/app.jar
ENV DB_URL="jdbc:h2:file:/app/data/support-tickets;DB_CLOSE_ON_EXIT=FALSE"
EXPOSE 8080
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
