FROM tomcat:8.5-jdk17-temurin
COPY target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
RUN mkdir /storedcredential
CMD cp /credentials/StoredCredential /storedcredential/StoredCredential && catalina.sh run

