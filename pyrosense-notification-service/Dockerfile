FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY pyrosense-shared-kernel/pom.xml ./pyrosense-shared-kernel/pom.xml
COPY pyrosense-notification-service/pom.xml ./pyrosense-notification-service/pom.xml

RUN apk add --no-cache maven \
    && mvn dependency:go-offline -pl pyrosense-shared-kernel,pyrosense-notification-service -am -q -B 2>/dev/null || true

COPY pyrosense-shared-kernel/src ./pyrosense-shared-kernel/src
COPY pyrosense-notification-service/src ./pyrosense-notification-service/src

RUN mvn package -pl pyrosense-shared-kernel,pyrosense-notification-service -am -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true -q -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S pyrosense && adduser -S pyrosense -G pyrosense
COPY --from=build /workspace/pyrosense-notification-service/target/*.jar app.jar
RUN chown -R pyrosense:pyrosense /app

USER pyrosense
EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
  CMD wget --quiet --tries=1 --spider http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
