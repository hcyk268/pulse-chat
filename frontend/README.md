# Trader Hub frontend

React 18 frontend built with Vite. The production build intentionally fails when
`VITE_API_BASE_URL` is missing or points to localhost.

## What is wired to the backend

| Screen | Data source |
| --- | --- |
| Market, coin detail, watchlist | `/api/v1/market/**` plus the `/topic/market/**` STOMP topics |
| Chat workspace | `/api/v1/conversations`, `/api/v1/messages`, `/user/queue/events` |
| Auth, profile | `/api/v1/auth/**`, `/api/v1/users/me` (access tokens refresh automatically) |
| Home feed, communities, notifications | Mock data in `src/data/traderHubData.js` (no backend endpoint yet) |

Mock-backed screens keep their state in Redux slices, so interactions such as
posting, joining a group, or marking a notification read behave like the real
thing and can be swapped to an API without touching the components.

## Admin console

`/admin` holds an operator console: overview, users, moderation queue, communities
and an audit log. The backend exposes no admin endpoints, so it runs entirely on
`src/data/adminMockData.js` through `adminSlice` — actions such as suspending a
user or resolving a report mutate local state and append an audit entry, which is
what the real flow would do. The shell says "sample data" on screen so it can
never be mistaken for live operations.

Row shapes match what an API would plausibly return, so wiring it up later means
replacing the reducers with thunks, not rewriting the tables.

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

Mock **content** (post bodies, news, community descriptions) stays in English:
it stands in for user-generated content, which is never translated.

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
