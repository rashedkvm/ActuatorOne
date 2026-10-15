FROM amazoncorretto:17-al2-jdk
EXPOSE 8080
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
USER 1000
ENTRYPOINT ["java","-jar","/app.jar"]
