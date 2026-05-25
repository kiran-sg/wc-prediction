# Use Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk AS build

# Set the working directory
WORKDIR /app

# Copy the Maven project files
COPY . .

# Build the application using Maven
RUN ./mvnw clean package -DskipTests

# Use a minimal JDK runtime for the final image
FROM eclipse-temurin:17-jre AS runtime

# Set the working directory
WORKDIR /app

# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
