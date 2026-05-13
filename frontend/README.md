# Frontend README

The frontend is a React application built with Vite. It provides the user interface for Monster Hunting Board and communicates with the Spring Boot backend through Axios.

---

## Technology

- React
- Vite
- Axios
- React Router
- ESLint
- Nginx for the production frontend container

---

## Structure

Frontend source code is under:

```text
frontend/src
```

Important areas:

- `src/api`: Axios client and API-specific request helpers
- `src/components`: reusable UI components and panels
- `src/pages`: route-level page components
- `src/hooks`: frontend hooks
- `src/assets`: frontend assets used by the UI

Container files:

- `frontend/Dockerfile`
- `frontend/nginx.conf`

---

## Install Dependencies

From the `frontend` folder:

```powershell
npm install
```

For CI-like installs, use:

```powershell
npm ci
```

---

## Run in Development Mode

Start the frontend dev server:

```powershell
npm run dev
```

Default URL:

```text
http://localhost:5173
```

The backend should also be running on:

```text
http://localhost:8080
```

The frontend uses `/api` as the default API base. If you run Vite directly on your host machine and need to bypass the proxy, create `frontend/.env.local` with:

```env
VITE_API_URL=http://localhost:8080/api
```

Then browser requests go directly to the backend on `localhost:8080`.

---

## API Configuration

The Axios client is configured in:

```text
src/api/apiClient.js
```

It uses:

```javascript
import.meta.env.VITE_API_URL || '/api'
```

This means:

- if `VITE_API_URL` is set, that value is used
- otherwise, requests go to `/api`

The `/api` fallback works for Docker production through Nginx and for bundled Spring Boot mode.

Most local and Docker workflows do not need this variable.

---

## Build the Frontend

From the `frontend` folder:

```powershell
npm run build
```

The Vite build output is written to:

```text
dist
```

Spring Boot can serve frontend files from:

```text
../src/main/resources/static
```

Vite does not automatically write to that folder. To refresh the bundled Spring Boot static files manually:

```powershell
npm run build
Remove-Item -Recurse -Force ..\src\main\resources\static\*
Copy-Item -Recurse .\dist\* ..\src\main\resources\static\
```

After copying the files, the Spring Boot backend can serve the built frontend on:

```text
http://localhost:8080
```

---

## Preview Production Build

From the `frontend` folder:

```powershell
npm run preview
```

This previews the built Vite output locally.

---

## Lint

From the `frontend` folder:

```powershell
npm run lint
```

This is also run by GitHub Actions.

---

## Docker

The frontend Docker image is built from:

```text
frontend/Dockerfile
```

It uses a multi-stage build:

1. Node builds the Vite app.
2. Nginx serves the built static files.

---

## Nginx

The production frontend container uses:

```text
frontend/nginx.conf
```

Nginx serves the React app and proxies backend-related paths to the backend container:

- `/api/...` -> `http://backend:8080`
- `/swagger-ui.html` -> `http://backend:8080`
- `/swagger-ui/...` -> `http://backend:8080`
- `/v3/api-docs` and `/v3/api-docs/...` -> `http://backend:8080`

The frontend uses client-side routing. Nginx falls back to `index.html` for normal frontend routes.

---

## Available Scripts

- `npm run dev`: start the Vite development server
- `npm run build`: build production assets
- `npm run preview`: preview the production build
- `npm run lint`: run ESLint

---

## Published Image

GitHub Actions publishes the frontend image to GitHub Container Registry:

```text
ghcr.io/demonetic/monster-hunter-board-frontend:latest
ghcr.io/demonetic/monster-hunter-board-frontend:<commit-sha>
```

The Hetzner production server pulls this image through `docker-compose.prod.yml`.
