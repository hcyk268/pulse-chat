# Backend IP rate limiting

## Request path

```text
Client -> Spring Boot :8080 -> Redis :6379
```

Spring Boot is published directly on `${SERVER_PORT:-8080}`. PostgreSQL and
Redis stay on the private `data` network. The backend also joins the `app`
network so it can make outbound calls to SendGrid and other external services.

The application resolves the client from `HttpServletRequest.getRemoteAddr()`.
Forwarded headers are disabled by default, so a caller cannot spoof its IP with
`X-Forwarded-For`.

## Backend limits

The limits are declared per endpoint with `@RateLimit` and shared across all
backend instances through Redis. The Redis Lua script implements a sliding
window and uses Redis server time, so decisions do not depend on JVM clock
skew.

Current authentication limits per client IP:

- Register: 5 requests per 5 minutes.
- Login: 5 requests per minute.
- Refresh token: 10 requests per minute.
- Forgot password: 3 requests per 5 minutes.
- Reset password: 5 requests per 5 minutes.
- Verify email: 10 requests per 5 minutes.
- Resend verification email: 3 requests per 5 minutes.
- Change password: 5 requests per 5 minutes.

When a limit is exceeded:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: <seconds>
Cache-Control: no-store
```

If Redis cannot produce a rate-limit decision, protected operations fail closed
with `503 Service Unavailable`.

## Adding a reverse proxy later

With the default `SERVER_FORWARD_HEADERS_STRATEGY=NONE`, Spring sees the proxy
address rather than the original client address. All clients behind that proxy
would therefore share one rate-limit bucket.

Before deploying behind Nginx, a load balancer, or a CDN:

1. Normalize the client IP only at that trusted edge.
2. Overwrite, rather than append, untrusted incoming forwarding headers.
3. Configure Spring/Tomcat to trust only the proxy's exact address or private
   CIDR.
4. Keep `ClientIpResolver` reading `request.getRemoteAddr()`; do not parse
   `X-Forwarded-For` in application code.

Do not enable native forwarded-header processing until the trusted proxy range
has been configured.
