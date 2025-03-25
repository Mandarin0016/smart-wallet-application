FROM bellsoft/liberica-openjdk-alpine:17

COPY target/smart-wallet-application-*.jar app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]