# Trader Hub frontend

React 18 frontend built with Vite. The production build intentionally fails when
`VITE_API_BASE_URL` is missing or points to localhost.

## What is wired to the backend

| Screen | Data source |
| --- | --- |
| Market, coin detail, watchlist | `/api/v1/market/**` plus the `/topic/market/**` STOMP topics |
| Chat workspace | `/api/v1/conversations`, `/api/v1/messages`, `/user/queue/events` |
| Auth, profile | `/api/v1/auth/**`, `/api/v1/users/me` (access tokens refresh automatically) |
| Communities | `/api/v1/community/**` plus its realtime events |
| Notifications | `/api/v1/notifications/**` plus `/user/queue/events` |
| Home feed | Local sample content in `src/data/traderHubData.js` |

## Admin console

The operator console uses local sample data because the backend exposes no admin
contract or authorization role. It is excluded from routes and navigation by
default. Set `VITE_ENABLE_ADMIN_DEMO=true` only for an explicit demo build.

The mock pages remain available for UI development, but must not be enabled in a
normal production deployment.

## Quality gates in detail

`npm run check` runs lint, unit tests, the render smoke test and a production
build. `npm run test:render` is the one worth knowing about: it bundles the real
pages for Node and renders **every route in every language, signed in and signed
out**, asserting nothing crashes, nothing renders empty, and no translation key
leaks into the markup.

## Copy and languages

All UI copy lives in `src/i18n/locales/{en,vi}.js` as flat dot-separated keys.
Components read it through `useTranslation()`; nothing user-visible is written
inline. `npm test` fails if the two catalogues drift apart, if a translation
loses an interpolation hole, or if a hardcoded string reappears in a component.

The chosen language is stored under `chatapp.locale` and sent to the backend as
`Accept-Language`, so server messages come back from `messages_{en,vi}.properties`
in the same language. Numbers, dates and relative times follow the locale too.

Sample **content** (post bodies and news) stays in English: it stands in for
user-generated content, which is never translated.

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
VITE_APP_ORIGIN=https://app.example.com \
npm run check
```

On PowerShell:

```powershell
$env:VITE_API_BASE_URL = "https://api.example.com"
$env:VITE_WS_URL = "wss://api.example.com/ws"
$env:VITE_APP_ORIGIN = "https://app.example.com"
npm run check
```

## Container build

```bash
docker build \
  --build-arg VITE_API_BASE_URL=https://api.example.com \
  --build-arg VITE_WS_URL=wss://api.example.com/ws \
  --build-arg VITE_APP_ORIGIN=https://app.example.com \
  --build-arg VITE_ENABLE_ADMIN_DEMO=false \
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
