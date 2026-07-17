# Monster Hunting Board

Monster Hunting Board is a full-stack web application for managing fantasy hunts, hunters, beasts, inventory, battles, and weather-influenced hunt results.

The project was built for a DevOps assignment and includes local development workflows, Dockerized runtime, CI checks, image publishing, and automated deployment to a Hetzner server.

---

## Stack

- Backend: Spring Boot 3, Java 21, Spring Security, JWT, Spring Data JPA
- Frontend: React, Vite, Axios
- Database: MySQL in Docker
- Tests: Maven, Spring Boot Test, H2
- API docs: Swagger UI / OpenAPI
- Weather service: Open-Meteo, called from the backend
- DevOps: Docker, Docker Compose, GitHub Actions, GHCR, Hetzner

---

## Project Guides

- [src/README.md](src/README.md): backend setup, configuration, API, and tests
- [frontend/README.md](frontend/README.md): frontend setup, development, build, and Nginx behavior

Important root files:

- [docker-compose.yml](docker-compose.yml): local full-stack Docker setup
- [docker-compose.prod.yml](docker-compose.prod.yml): production Docker setup for Hetzner
- [generated-requests.http](generated-requests.http): manual API requests
- [requirements.md](requirements.md): assignment requirements

---

## Quick Start With Docker

Create a local `.env` file:

```powershell
Copy-Item .env.example .env
```

Start the full application:

```powershell
docker compose up --build
```

Open:

- App: `http://localhost`
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost/swagger-ui.html`

Stop containers:

```powershell
docker compose down
```

---

## Other Local Workflows

Backend only:

```powershell
mvn spring-boot:run
```

Frontend dev server:

```powershell
cd frontend
npm install
npm run dev
```

For more detail, use the backend and frontend README files linked above.

---

## Production

The application is deployed on Hetzner:

```text
https://monster-hunter-board.duckdns.org/
```

Production uses [docker-compose.prod.yml](docker-compose.prod.yml) and pulls images from GitHub Container Registry:

- `ghcr.io/demonetic/monster-hunter-board-backend:latest`
- `ghcr.io/demonetic/monster-hunter-board-frontend:latest`

Manual production update on the server:

```bash
cd ~/git/06_devops_assignment_1_individual
git pull origin main
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --force-recreate
docker compose -f docker-compose.prod.yml ps
```

Public production URLs:

- App: `https://monster-hunter-board.duckdns.org/`
- API example: `https://monster-hunter-board.duckdns.org/api/beasts`
- Swagger UI: `https://monster-hunter-board.duckdns.org/swagger-ui.html`
- OpenAPI JSON: `https://monster-hunter-board.duckdns.org/v3/api-docs`

---

## CI/CD

> **Portfolio mirror:** GitHub Actions is intentionally disabled in this public repository to prevent the included publishing and deployment workflows from affecting the original production environment. The workflow files are retained to demonstrate the project's CI/CD implementation.

GitHub Actions workflows:

- [ci.yml](.github/workflows/ci.yml): backend tests, frontend lint/build, Docker build checks, and Compose validation
- [publish-images.yml](.github/workflows/publish-images.yml): publishes backend and frontend images to GHCR
- [deploy.yml](.github/workflows/deploy.yml): deploys the latest images to Hetzner over SSH

Original pipeline:

```text
Push to main
-> CI validates the project
-> Docker images are published to GHCR
-> Hetzner pulls and recreates the production containers
```

Required GitHub Actions secrets:

- `HETZNER_HOST`
- `HETZNER_USER`
- `HETZNER_SSH_KEY`
- `HETZNER_PORT`

---

## Main Features

- JWT login and role-based access
- Hunter registration and profile management
- Beast and hunt management
- Solo and group hunt flows
- Inventory and shop functionality
- Weather-aware hunt calculations with Open-Meteo data
- Swagger/OpenAPI documentation
