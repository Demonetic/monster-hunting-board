# Backend README

## Overview

The backend is a Spring Boot application that provides:

- JWT-based authentication
- REST endpoints under `/api`
- MySQL support for the main application
- H2-based automated tests

The backend source code is located under `src/main/java`, and runtime configuration is primarily defined in `src/main/resources/application.properties`.

## Prerequisites

- Java 21
- A running MySQL instance for normal application startup
- Docker Desktop or another Docker engine for containerized startup

## Run the Backend

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

Default URL:

- Application root: `http://localhost:8080/`
- API base: `http://localhost:8080/api`

## Run the Backend with Docker Compose

From the repository root:

```powershell
docker compose up --build
```

This starts:

- the Spring Boot application
- a MySQL database container used by the application

The backend is then available through:

- `http://localhost:8080/`
- `http://localhost:8080/api`

Stop the stack with:

```powershell
docker compose down
```

## Run Tests

Run the full backend test suite from the repository root:

```powershell
.\mvnw.cmd clean test
```

The test suite uses `src/test/resources/application-test.properties`, which is intentionally isolated from local `.env` values.

## Configuration

Main application settings are in:

- `src/main/resources/application.properties`
- `.env.example`

Sensitive or environment-specific values are read through environment variables, including:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`

In Docker Compose, these values are supplied directly through `docker-compose.yml`. The backend connects to MySQL with:

- `DB_URL=jdbc:mysql://db:3306/monster_hunting_board`

## API Endpoints

Main controller areas:

- `/api/auth`
- `/api/beasts`
- `/api/hunts`
- `/api/hunters`

Swagger UI is available at:

- `http://localhost:8080/swagger-ui.html`

## Using generated-requests.http

The file [generated-requests.http](../generated-requests.http) contains ready-made HTTP requests for manual API testing.

It includes flows for:

- login
- token handling
- hunter profile requests
- beast CRUD
- hunt CRUD
- joining and completing hunts

The file is intended for IDE HTTP clients such as IntelliJ IDEA's built-in HTTP client.

Typical usage:

1. Start the backend.
2. Open `generated-requests.http`.
3. Run the requests in order from top to bottom.

The file uses:

- `@baseUrl = http://localhost:8080`
- saved JWT tokens for different users
- generated IDs shared between requests
