FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -q
COPY backend/src ./src
RUN mvn package -Dmaven.test.skip=true -q
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system sama && useradd --system --gid sama sama
COPY --from=build /app/target/sama-0.1.0.jar app.jar
USER sama
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
