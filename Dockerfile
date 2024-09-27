FROM openjdk:17-oracle
ARG JAR_FILE=/build/libs/k8sTestApp.jar
COPY ${JAR_FILE} app.jar
ENV username default
EXPOSE 8080
VOLUME ["logs"]
ENTRYPOINT ["java", "-Dconst.username=${username}","-jar", "app.jar"]
