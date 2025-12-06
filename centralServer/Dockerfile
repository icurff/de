FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Install necessary tools
RUN apk add --no-cache bash

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
RUN chmod +x mvnw
COPY pom.xml .

# Copy .mvn directory
COPY .mvn .mvn

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Run the application
ENTRYPOINT ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]

