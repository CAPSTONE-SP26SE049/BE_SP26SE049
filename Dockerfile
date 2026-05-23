# Use Eclipse Temurin for Java 17 (Maven build stage)
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy maven executable to the image
COPY mvnw .
COPY .mvn .mvn

# Copy the pom.xml file
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (this step is cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy the project source
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Use a smaller JRE image for runtime
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Cloud Run defaults to 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
