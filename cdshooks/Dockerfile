FROM openjdk:8
VOLUME /tmp
ADD target/cds-hooks-0.1.8-SNAPSHOT-exec.jar cds-hooks-0.1.8-SNAPSHOT-exec.jar
ENTRYPOINT ["java","-jar","cds-hooks-0.1.8-SNAPSHOT-exec.jar"]