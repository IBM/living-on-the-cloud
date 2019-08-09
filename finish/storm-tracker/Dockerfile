FROM adoptopenjdk/openjdk8-openj9:alpine-slim

COPY target/storm-tracker.jar /

RUN mkdir /root/.postgresql/

COPY root.crt /root/.postgresql/

ENTRYPOINT ["java", "-jar", "storm-tracker.jar" ]