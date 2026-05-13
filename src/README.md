# Backend README

The backend is a Spring Boot application that exposes the REST API for Monster Hunting Board.

It handles authentication, authorization, hunters, beasts, hunts, battle results, inventory, shop actions, and weather data. The backend is also responsible for calling the external weather service; the frontend only displays the weather-related data returned by the API.

---

## Technology

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- JWT authentication
- Spring Data JPA
- MySQL for normal runtime
- H2 for automated tests
- Springdoc OpenAPI / Swagger UI
- Open-Meteo API for current weather and geocoding

---

## Structure

Main backend code is under:

```text
src/main/java/se/edugrade/monsterhuntingboard
```

Important areas:

- `controller`: REST controllers
- `service`: application logic
- `model`: JPA entities and domain models
- `repository`: Spring Data repositories
- `dto`: request and response objects
- `config`: security and OpenAPI configuration
- `security`: JWT filter and token handling

Important resources:

- `src/main/resources/application.properties`
- `src/main/resources/data.sql`
- `src/main/resources/static`

- `src/test/resources/application-test.properties`

---

## Configuration

The backend reads environment variables through `application.properties`.

Main variables:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`

For Docker Compose, these are normally provided through the root `.env` file.

Default local values exist in `application.properties`, but real secrets should be supplied as environment variables in production.

---

## Database

The normal application database is MySQL.

In Docker Compose, the backend connects to the database service by hostname:

```text
jdbc:mysql://db:3306/monster_hunting_board
```

Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

The file `data.sql` seeds baseline users and game data. It is written to be repeatable where possible, using duplicate-key handling and existence checks.

---

## Run Backend Locally

Start MySQL first. The easiest way is from the repository root:

```powershell
docker compose up db
```

Then run the backend from the repository root:

```powershell
mvn spring-boot:run
```

On Windows, the included Maven wrapper command also works:

```powershell
.\mvnw.cmd spring-boot:run
```

Default URLs:

- Application root: `http://localhost:8080`
- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## Run Backend Tests

Run all backend tests from the repository root:

```powershell
mvn test
```

Or on Windows:

```powershell
.\mvnw.cmd test
```

The test suite uses H2 through:

```text
src/test/resources/application-test.properties
```

Tests do not require the local `.env` file or a running MySQL container.

---

## API

Main endpoint groups:

- `/api/auth`: login, registration, and appearance options
- `/api/beasts`: beast listing and beast management
- `/api/hunts`: hunt listing, creation, joining, solo/group battles, and completion
- `/api/hunters`: current hunter profile, admin hunter data, inventory, shop, and location updates
- `/api/weather`: current weather for the authenticated hunter

Most mutating operations and hunter-specific data require JWT authentication:

```http
Authorization: Bearer <token>
```

Tokens are returned from the auth endpoints and sent as bearer tokens from the frontend.

---

## Weather Feature

The backend integrates with Open-Meteo:

- geocoding: city name to latitude/longitude
- forecast: current weather code, wind speed, and temperature

Weather is resolved for a hunter's saved city. The backend maps Open-Meteo data into game-specific weather categories and modifiers used by hunt and battle calculations.

The current weather endpoint is:

```text
GET /api/weather/current
```

It requires an authenticated hunter.

---

## Swagger / OpenAPI

Swagger UI is available locally at:

```text
http://localhost:8080/swagger-ui.html
```

The raw OpenAPI document is available at:

```text
http://localhost:8080/v3/api-docs
```

---

## Manual API Testing

The root file [generated-requests.http](../generated-requests.http) contains ready-made requests for IDE HTTP clients such as IntelliJ IDEA.

It includes login, JWT reuse, hunter requests, beast CRUD, hunt CRUD, joining hunts, and battle flows.

Typical usage:

1. Start the backend.
2. Open `generated-requests.http`.
3. Run requests in order.

---

## Docker

The backend image is built from the root [Dockerfile](../Dockerfile). The root README describes the full Docker Compose and deployment flow.
