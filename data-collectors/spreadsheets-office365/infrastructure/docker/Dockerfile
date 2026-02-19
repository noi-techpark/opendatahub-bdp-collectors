FROM maven:3-eclipse-temurin-8-alpine as base
COPY target/ROOT.jar app.jar
CMD java $JAVA_OPTS -jar app.jar