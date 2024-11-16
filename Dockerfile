# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY build/libs/map-0.0.1-SNAPSHOT.jar app.jar

# Expose the port (optional, as Render doesn't rely on this for detection)
EXPOSE 8080

# Run the JAR file, dynamically binding the PORT environment variable
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
