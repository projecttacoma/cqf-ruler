FROM openjdk:8
VOLUME /tmp
ADD target/dstu2-0.1.8-SNAPSHOT-exec.jar dstu2-0.1.8-SNAPSHOT-exec.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","dstu2-0.1.8-SNAPSHOT-exec.jar"]