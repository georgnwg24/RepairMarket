FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml checkstyle.xml ./
COPY src ./src
RUN mvn -B -DskipTests -Dmaven.gitcommitid.skip=true package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home app
WORKDIR /app
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p database uploads log && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
