FROM farao/farao-computation-base:1.4.0

ARG JAR_FILE=rao-runner-app/target/*.jar
COPY ${JAR_FILE} app.jar
COPY .itools /home/farao/.itools

ENTRYPOINT ["java", "-jar", "/app.jar"]