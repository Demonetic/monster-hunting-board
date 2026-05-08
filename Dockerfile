FROM maven:3.9.9-amazoncorretto-21-debian AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM amazoncorretto:21.0.5-alpine
WORKDIR /app

RUN apk --no-cache add curl

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]