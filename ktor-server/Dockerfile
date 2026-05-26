FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/ktor-server-all.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
