# Frontend README

## Overview

The frontend is a React application built with Vite. It communicates with the backend API through Axios and supports both:

- local development with the Vite dev server
- bundled production builds served by Spring Boot

For Docker-based startup, the frontend is not run as a separate container. It is built and bundled into the Spring Boot application through the root-level `Dockerfile`.

## Prerequisites

- Node.js
- npm

## Install Dependencies

From the `frontend` folder:

```powershell
npm install
```

## Run the Frontend in Development Mode

Start the Vite dev server:

```powershell
npm run dev
```

Default frontend URL:

- `http://localhost:5173`

API behavior in development:

- the frontend uses `/api` by default
- Vite proxies `/api` requests to `http://localhost:8080`

This means the backend should also be running during frontend development.

## Build the Frontend for Bundled Mode

Build the frontend into the Spring Boot static resource directory:

```powershell
npm.cmd run build
```

The production build output is written to:

- `../src/main/resources/static`

After building, start the backend from the repository root and open:

- `http://localhost:8080`

## Frontend in Docker Mode

When starting the application with:

```powershell
docker compose up --build
```

the frontend is built during the Docker image build and then served by Spring Boot on:

- `http://localhost:8080`

In that mode, you do not need to run Vite separately.

## Environment Variables

Frontend environment variables use the Vite `VITE_` prefix.

Relevant variable:

- `VITE_API_URL`

Example:

```env
VITE_API_URL=http://localhost:8080/api
```

If `VITE_API_URL` is not set, the frontend falls back to:

```text
/api
```

That fallback works for:

- bundled mode in Spring Boot
- local development through the Vite proxy
- Docker mode, where the frontend is served from the same origin as the backend

## Available Scripts

- `npm run dev`: start the Vite development server
- `npm.cmd run build`: create a production build for Spring Boot
- `npm run preview`: preview the production build with Vite
- `npm run lint`: run ESLint
