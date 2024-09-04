FROM openjdk:21-jdk

ARG JAR_FILE=/build/libs/k8sTestApp-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar
ENV username default

EXPOSE 8080

VOLUME ["logs"]

ENTRYPOINT ["java", "-Dconst.username=${username}","-jar", "app.jar"]
