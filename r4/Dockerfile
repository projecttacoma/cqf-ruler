FROM openjdk:8
VOLUME /tmp
ADD target/r4-0.1.8-SNAPSHOT-exec.jar r4-0.1.8-SNAPSHOT-exec.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","r4-0.1.8-SNAPSHOT-exec.jar"]