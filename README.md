# Monster Hunting Board

## Overview

Monster Hunting Board is a full-stack web application built with:

- Spring Boot for the backend API
- React + Vite for the frontend
- MySQL for the main application database
- H2 for automated tests

The application supports authentication with JWT, hunter and hunt management, and a bundled one-port mode where Spring Boot serves both the frontend and the API on `http://localhost:8080`.

## Repository Structure

- [src/README.md](src/README.md): backend setup, run commands, tests, and API request file usage
- [frontend/README.md](frontend/README.md): frontend setup, development mode, and production build workflow
- [generated-requests.http](generated-requests.http): sample API requests for manual backend testing
- [requirements.md](requirements.md): assignment requirements
- [documentation/](documentation/intended-layout-design.md): design notes and screenshots

## Main Workflows

### Backend Only

Run the Spring Boot application from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend API will be available on `http://localhost:8080/api/...`.

### Frontend Development

Run the frontend dev server from the `frontend` folder:

```powershell
cd frontend
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` requests to the backend.

### Bundled Full App

Build the frontend into Spring Boot static resources, then start the backend:

```powershell
cd frontend
npm.cmd run build
cd ..
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`.

## Environment Configuration

Copy `.env.example` to `.env` and fill in your local values for the main application.

Example values include:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`
- `VITE_API_URL`

Tests do not require `.env`. They use isolated H2 settings from `src/test/resources/application-test.properties`.
