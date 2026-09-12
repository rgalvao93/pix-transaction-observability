FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd -f app && \
    useradd --create-home -g app --shell /usr/sbin/nologin app
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:MaxMetaspaceSize=256m -XX:+UseG1GC"

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8081}"]