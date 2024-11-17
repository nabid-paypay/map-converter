# First stage: Build the JAR file
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace
COPY . .
RUN ./gradlew bootJar

# Second stage: Run the application
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/map-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
