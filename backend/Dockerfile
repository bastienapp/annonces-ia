FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -q verify

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY certs/netskope-ca.crt /tmp/netskope-ca.crt
RUN keytool -importcert -noprompt \
    -alias maif-ca \
    -file /tmp/netskope-ca.crt \
    -cacerts -storepass changeit \
    && rm /tmp/netskope-ca.crt

COPY --from=build /app/target/annonces-ia-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
