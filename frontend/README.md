# Pulse Chat frontend

React 18 frontend built with Vite. The production build intentionally fails when
`VITE_API_BASE_URL` is missing or points to localhost.

## Local development

```bash
npm ci
npm run dev
```

Development defaults to `http://localhost:8080`. Copy `.env.example` to `.env`
only when a different local backend or WebSocket endpoint is needed. Local
`.env` files are ignored by git.

## Quality gates

```bash
npm run lint
npm test
npm run check
```

`npm run check` includes a production build, so provide production-like
endpoints:

```bash
VITE_API_BASE_URL=https://api.example.com \
VITE_WS_URL=wss://api.example.com/ws \
npm run check
```

On PowerShell:

```powershell
$env:VITE_API_BASE_URL = "https://api.example.com"
$env:VITE_WS_URL = "wss://api.example.com/ws"
npm run check
```

## Container build

```bash
docker build \
  --build-arg VITE_API_BASE_URL=https://api.example.com \
  --build-arg VITE_WS_URL=wss://api.example.com/ws \
  -t pulse-chat-frontend .
```

The Nginx image includes SPA fallback routing, immutable asset caching, a
`/healthz` endpoint, CSP, and baseline security headers.

## Authentication boundary

The current backend returns access and refresh tokens in JSON, so the frontend
must persist them in browser storage to preserve existing behavior. The storage
layer validates refresh-token expiry and handles blocked storage safely. Moving
the refresh token to a `Secure`, `HttpOnly`, `SameSite` cookie requires a
coordinated backend contract change.
