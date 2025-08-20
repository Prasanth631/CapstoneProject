FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/login-app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
