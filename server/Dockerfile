FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /source
COPY pom.xml .
COPY .mvn/settings.xml /root/.m2/settings.xml
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir /app/data && chown -R 10001:10001 /app
USER 10001:10001
COPY --from=build /source/target/piaochu-server-1.0.0.jar /app/server.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
