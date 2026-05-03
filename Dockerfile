FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build -- --outDir dist

FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app

COPY pom.xml ./
COPY src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=backend-builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
