FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/pyrosense-iot-simulator-1.0.0.jar simulator.jar

ENTRYPOINT ["java", "-jar", "simulator.jar"]
CMD ["--mqtt-host", "mosquitto", "--mqtt-port", "1883", "--tenants", "2", "--buildings", "2", "--devices", "3"]
