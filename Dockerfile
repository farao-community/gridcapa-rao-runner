FROM farao/farao-computation-base:1.9.0 AS BUILDER
ARG JAR_FILE=rao-runner-app/target/*.jar
COPY ${JAR_FILE} app.jar
COPY .itools /home/farao/.itools
RUN mkdir -p /tmp/app  \
    && java -Djarmode=tools  \
    -jar /app.jar extract --layers --launcher \
    --destination /tmp/app

FROM farao/farao-computation-base:1.9.0
COPY .itools /home/farao/.itools
COPY --from=BUILDER /tmp/app/dependencies/ ./
COPY --from=BUILDER /tmp/app/spring-boot-loader/ ./
COPY --from=BUILDER /tmp/app/application/ ./
COPY --from=BUILDER /tmp/app/snapshot-dependencies/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]