# TraderHub - Real-time Chat & Trading Platform

A real-time financial and crypto communication platform featuring live market streaming, AI-powered assistance, community discussions, and high-performance WebSocket messaging.

---

## 1. Backend

### Tech Stack
- **Language & Framework**: Java 17, Spring Boot 3.5.x, Spring Security, Spring WebSocket (STOMP), Spring Data JPA.
- **Database & Caching**: PostgreSQL 16 (Flyway migrations), Redis 7 (caching, session state, rate limiting).
- **AI & Document Processing**: Spring AI (OpenAI Compatible).
- **Storage & Mail**: Cloudflare R2 / AWS S3 (Presigned URLs), SendGrid API.
- **Observability & Metrics**: Spring Boot Actuator, Micrometer Prometheus, Grafana.
- **API Documentation**: OpenAPI 3 / Swagger UI (`/swagger-ui.html`).

### Core Modules & Architecture
1. **Authentication & User Management**:
   - JWT authentication with Access & Refresh Token rotation and secure hashing.
   - SendGrid email verification, password reset flows, profile management, and Redis-cached user session details.
2. **Real-time Chat & Messaging**:
   - WebSocket STOMP protocol (`/ws`) supporting 1-on-1 and community group conversations.
   - Rich messages with media attachments, reply threads, message pinning, emoji reactions, read receipts, and live typing indicators.
3. **Market & Crypto Stream**:
   - Real-time market ticker and candlestick (kline) data synchronization and streaming.
   - Redis caching for market overviews and coin details, customizable watchlists, and user-defined price alerts.
4. **Community & Social Feeds**:
   - Discussion channels, posts, threaded comments, reactions, tag categorizations, and role-based permissions.
5. **AI Assistant**:
   - Spring AI integration (OpenAI GPT) for contextual chat and automated document/image analysis.
   - Redis-backed conversation memory and granular rate limiting.
6. **Transactional Outbox & Media Storage**:
   - Transactional Outbox Pattern to guarantee event delivery for real-time notifications and system events.
   - Secure direct uploads to S3/R2 via Presigned URLs with Apache Tika content validation.

### Backend Setup & Execution

#### Run with Docker Compose
```bash
cd backend
docker compose up --build -d
```
*Default ports:*
- Backend API: `http://localhost:8080` (Swagger UI: `http://localhost:8080/swagger-ui.html`)
- PostgreSQL: `localhost:5433`
- Redis: `localhost:6380`
- Prometheus: `localhost:9090` | Grafana: `localhost:3000`


---

## 2. Frontend

### Tech Stack
- React 18, Vite, Redux Toolkit, TradingView Lightweight Charts, Lucide Icons.

### Quick Start
```bash
cd frontend
npm ci
npm run dev
```
- Web App URL: `http://localhost:5173` (defaults to connecting with backend at `http://localhost:8080`).
