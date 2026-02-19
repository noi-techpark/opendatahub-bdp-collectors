FROM maven:3-openjdk-17-slim as base
COPY target/ROOT.jar app.jar
CMD java $JAVA_OPTS -jar app.jar