# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# komplette API-Struktur rein (damit parent POM vorhanden ist)
COPY api/ api/

# Dependencies cachen (Reactor-Build: Aggregator-POM, Modul api-app)
RUN mvn -q -f api/pom.xml -DskipTests -pl api-app -am dependency:go-offline

# Build
RUN mvn -q -f api/pom.xml -DskipTests -pl api-app -am clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
# Spring Boot JAR aus api-app übernehmen (Version ist SNAPSHOT → Wildcard)
COPY --from=build /build/api/api-app/target/*-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
