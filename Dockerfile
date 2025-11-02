FROM maven:3.8.8-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -T1C -DskipTests clean package -DskipTests=true
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java -jar /app/app.jar"]
