FROM maven:3-openjdk-17-slim as base
COPY target/ROOT.jar app.jar
CMD [ "java", "-jar", "app.jar" ]