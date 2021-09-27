FROM farao/farao-computation-base:1.2.0

ARG JAR_FILE=rao-runner-app/target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]