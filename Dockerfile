FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/innosistemas-back.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
