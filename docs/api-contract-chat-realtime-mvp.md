# API Contract Chi Tiết Cho App Chat Realtime MVP

## 1. Mục tiêu

Tài liệu này chốt contract giữa frontend và backend cho ứng dụng chat realtime cơ bản theo phạm vi MVP. Contract được tách thành 2 phần:

- REST API cho các thao tác bền vững: xác thực, hồ sơ người dùng, tìm kiếm user, tạo hội thoại, tải danh sách hội thoại, tải và gửi tin nhắn, cập nhật đã đọc
- Realtime API qua WebSocket/STOMP cho push event và tín hiệu tạm thời như typing, delivered, presence

OpenAPI cho phần REST được lưu tại:

- [docs/openapi/chat-rest-api.yaml](./openapi/chat-rest-api.yaml)

## 2. Quy ước chung

### 2.1. Base path

- REST base path: `/api/v1`
- WebSocket endpoint: `/ws`

### 2.2. Định dạng dữ liệu

- Tất cả request và response REST dùng `application/json`
- Tất cả thời gian trả về theo ISO-8601 UTC, ví dụ `2026-05-08T15:42:10Z`
- Tên field theo `camelCase`

### 2.3. Xác thực

- REST dùng header `Authorization: Bearer <accessToken>`
- WebSocket/STOMP gửi access token trong frame `CONNECT` qua native header `Authorization`
- `refreshToken` chỉ dùng cho endpoint làm mới token

### 2.4. Định danh

- `id` của entity lưu trong DB dùng kiểu số nguyên dương `Long`
- `clientMessageId` là chuỗi duy nhất do client sinh ra, khuyến nghị UUID, dùng để chống gửi trùng khi mạng chập chờn

### 2.5. Phân trang

- `GET /api/v1/conversations` dùng snapshot cursor pagination
- `GET /api/v1/users/search` và `GET /api/v1/messages?conversationId={conversationId}` dùng cursor pagination thông thường
- `cursor` là chuỗi opaque, client không tự parse
- `snapshotAt` là mốc thời gian do server trả về ở page đầu của danh sách hội thoại; client phải gửi lại nguyên giá trị đó cho mọi request load-more tiếp theo
- `limit` mặc định `20`, tối đa `100`

### 2.6. Trạng thái tin nhắn

- `SENT`: server đã lưu tin nhắn thành công
- `DELIVERED`: ít nhất một session của người nhận đã xác nhận nhận được tin
- `READ`: người nhận đã đánh dấu đã đọc

Luật chuyển trạng thái:

- `SENT -> DELIVERED -> READ`
- Không cho phép đi lùi trạng thái

## 3. Quyết định thiết kế contract

### 3.1. Hành động bền vững đi qua REST

Các thao tác sau phải gọi REST để đảm bảo dễ retry, dễ audit và trạng thái lưu bền:

- Đăng ký
- Đăng nhập
- Refresh token
- Cập nhật hồ sơ
- Tạo hoặc mở hội thoại 1-1
- Lấy danh sách hội thoại
- Lấy lịch sử tin nhắn
- Gửi tin nhắn
- Đánh dấu đã đọc


Quy tac hien thi hoi thoai direct trong MVP:

- Khi tao direct conversation moi ma chua co tin nhan nao, chi nguoi tao nhin thay hoi thoai trong danh sach chat
- Nguoi con lai chi nhin thay hoi thoai sau khi co tin nhan dau tien, hoac khi chinh ho chu dong mo lai direct conversation do
- `GET /api/v1/conversations` chi tra cac hoi thoai co `isVisibleInList = true` doi voi participant hien tai
### 3.2. Hành động tạm thời đi qua WebSocket

Các tín hiệu sau đi qua WebSocket/STOMP:

- Server push tin nhắn mới
- Server push thay đổi trạng thái tin nhắn
- Server push thay đổi hội thoại
- Server push presence
- Server push typing
- Client gửi typing start hoặc stop
- Client gửi delivered acknowledgement

### 3.3. Nguồn sự thật

- REST là nguồn sự thật cuối cùng cho dữ liệu bền vững
- Realtime chỉ dùng để giảm độ trễ hiển thị
- Nếu client bỏ lỡ event realtime, client phải đồng bộ lại bằng REST

### 3.4. Quy tắc snapshot cho danh sách hội thoại

- Danh sách hội thoại được sắp xếp theo `sortAt DESC, conversationId DESC`
- `sortAt` được xác định là `COALESCE(lastMessageAt, createdAt)`
- Ở page đầu của `GET /api/v1/conversations`, server trả thêm `snapshotAt`
- Mọi request load-more của cùng một phiên phân trang phải gửi lại đúng `snapshotAt` đó
- Nếu một hội thoại có tin nhắn mới sau `snapshotAt`, hội thoại đó không còn thuộc snapshot cũ và không bắt buộc xuất hiện ở các page tiếp theo
- Các thay đổi mới phát sinh sau `snapshotAt` phải đi vào UI qua realtime event hoặc bằng cách reload page đầu

## 4. Danh sách endpoint REST

| Method | Path | Mục đích | Auth |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản mới | Không |
| `POST` | `/api/v1/auth/login` | Đăng nhập bằng username hoặc email | Không |
| `POST` | `/api/v1/auth/refresh` | Làm mới access token bằng refresh token | Không |
| `GET` | `/api/v1/users/me` | Lấy hồ sơ của chính mình | Có |
| `PATCH` | `/api/v1/users/me` | Cập nhật hồ sơ cơ bản | Có |
| `GET` | `/api/v1/users/search` | Tìm kiếm user để bắt đầu chat | Có |
| `POST` | `/api/v1/conversations/direct` | Tạo hoặc mở hội thoại 1-1 | Có |
| `GET` | `/api/v1/conversations` | Lấy danh sách hội thoại | Có |
| `GET` | `/api/v1/conversations/{conversationId}` | Lấy chi tiết một hội thoại | Có |
| `GET` | `/api/v1/messages?conversationId={conversationId}` | Lấy lịch sử tin nhắn | Có |
| `POST` | `/api/v1/messages` | Gửi tin nhắn text | Có |
| `POST` | `/api/v1/messages/read` | Đánh dấu đã đọc đến một mốc tin | Có |

## 5. Contract chi tiết phần REST

### 5.1. Đăng ký

- Endpoint: `POST /api/v1/auth/register`
- Mục đích: tạo tài khoản mới và trả về token để client vào hệ thống ngay

Request:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "displayName": "Alice",
  "password": "Secret123!",
  "confirmPassword": "Secret123!"
}
```

Response `201`:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "tokenType": "Bearer",
  "accessTokenExpiresInMs": 3600000,
  "refreshTokenExpiresInMs": 604800000,
  "user": {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com",
    "displayName": "Alice",
    "avatarUrl": null,
    "bio": null,
    "accountStatus": "ACTIVE",
    "createdAt": "2026-05-08T15:42:10Z",
    "updatedAt": "2026-05-08T15:42:10Z"
  }
}
```

### 5.2. Đăng nhập

- Endpoint: `POST /api/v1/auth/login`
- Dùng `usernameOrEmail` để đăng nhập linh hoạt theo mô hình backend hiện tại

Request:

```json
{
  "usernameOrEmail": "alice",
  "password": "Secret123!"
}
```

Response `200`: cùng schema với đăng ký

### 5.3. Refresh token

- Endpoint: `POST /api/v1/auth/refresh`
- Chỉ nhận refresh token

Request:

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

Response `200`:

```json
{
  "accessToken": "new-jwt-access-token",
  "refreshToken": "new-jwt-refresh-token",
  "tokenType": "Bearer",
  "accessTokenExpiresInMs": 3600000,
  "refreshTokenExpiresInMs": 604800000,
  "user": {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com",
    "displayName": "Alice",
    "avatarUrl": null,
    "bio": null,
    "accountStatus": "ACTIVE",
    "createdAt": "2026-05-08T15:42:10Z",
    "updatedAt": "2026-05-08T15:42:10Z"
  }
}
```

### 5.4. Hồ sơ cá nhân

`GET /api/v1/users/me`

Response `200`:

```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "displayName": "Alice",
  "avatarUrl": "https://cdn.example.com/avatars/alice.png",
  "bio": "Hello there",
  "accountStatus": "ACTIVE",
  "createdAt": "2026-05-08T15:42:10Z",
  "updatedAt": "2026-05-08T16:03:55Z"
}
```

`PATCH /api/v1/users/me`

Request:

```json
{
  "displayName": "Alice Nguyen",
  "avatarUrl": "https://cdn.example.com/avatars/alice.png",
  "bio": "Building a realtime chat app"
}
```

Response `200`: cùng schema với `GET /users/me`

### 5.5. Tìm kiếm user

- Endpoint: `GET /api/v1/users/search?q=ali&limit=10`
- Không trả chính user hiện tại
- Chỉ trả tài khoản đang hoạt động

Response `200`:

```json
{
  "items": [
    {
      "id": 2,
      "username": "alice.team",
      "displayName": "Alice Team",
      "avatarUrl": null,
      "presence": {
        "isOnline": true,
        "lastActiveAt": "2026-05-08T16:05:00Z"
      },
      "directConversationId": 1001
    }
  ],
  "paging": {
    "limit": 10,
    "nextCursor": null,
    "hasMore": false
  }
}
```

### 5.6. Tạo hoặc mở hội thoại 1-1

- Endpoint: `POST /api/v1/conversations/direct`
- Nếu hội thoại đã tồn tại, server trả `200`
- Nếu hội thoại mới được tạo, server trả `201`
- Khi tao moi ma chua co tin nhan, participant cua nguoi goi hien tai co `isVisibleInList = true`, participant con lai co `isVisibleInList = false`

Request:

```json
{
  "targetUserId": 2
}
```

Response:

```json
{
  "id": 1001,
  "type": "DIRECT",
  "participants": [
    {
      "id": 1,
      "username": "alice",
      "displayName": "Alice",
      "avatarUrl": null,
      "presence": {
        "isOnline": true,
        "lastActiveAt": "2026-05-08T16:05:00Z"
      },
      "isVisibleInList": true
    },
    {
      "id": 2,
      "username": "bob",
      "displayName": "Bob",
      "avatarUrl": null,
      "presence": {
        "isOnline": false,
        "lastActiveAt": "2026-05-08T15:58:00Z"
      },
      "isVisibleInList": false
    }
  ],
  "otherParticipant": {
    "id": 2,
    "username": "bob",
    "displayName": "Bob",
    "avatarUrl": null,
    "presence": {
      "isOnline": false,
      "lastActiveAt": "2026-05-08T15:58:00Z"
    }
  },
  "unreadCount": 0,
  "lastMessage": null,
  "lastMessageAt": null,
  "createdAt": "2026-05-08T16:06:20Z",
  "updatedAt": "2026-05-08T16:06:20Z"
}
```

### 5.7. Danh sách hội thoại

- Endpoint page đầu: `GET /api/v1/conversations?limit=20`
- Endpoint load-more: `GET /api/v1/conversations?limit=20&cursor=opaque-token&snapshotAt=2026-05-11T08:00:00Z`
- Chi tra cac hoi thoai ma participant hien tai co `isVisibleInList = true`
- Trả danh sách theo `sortAt DESC, conversationId DESC`
- `sortAt = COALESCE(lastMessageAt, createdAt)`

Response `200`:

```json
{
  "items": [
    {
      "id": 1001,
      "type": "DIRECT",
      "otherParticipant": {
        "id": 2,
        "username": "bob",
        "displayName": "Bob",
        "avatarUrl": null,
        "presence": {
          "isOnline": false,
          "lastActiveAt": "2026-05-08T15:58:00Z"
        }
      },
      "unreadCount": 2,
      "lastMessage": {
        "id": 9001,
        "senderId": 2,
        "contentPreview": "Are you free today?",
        "status": "SENT",
        "createdAt": "2026-05-08T16:04:22Z"
      },
      "lastMessageAt": "2026-05-08T16:04:22Z",
      "createdAt": "2026-05-08T15:10:00Z",
      "updatedAt": "2026-05-08T16:04:22Z"
    }
  ],
  "paging": {
    "limit": 20,
    "nextCursor": "opaque-next-cursor",
    "hasMore": true,
    "snapshotAt": "2026-05-11T08:00:00Z"
  }
}
```

Quy tắc phân trang:

- Request đầu tiên không cần gửi `cursor` và `snapshotAt`
- Response đầu tiên phải trả `paging.snapshotAt`
- Từ request load-more thứ hai trở đi, nếu có `cursor` thì bắt buộc phải gửi lại đúng `snapshotAt` đã nhận ở page đầu
- `nextCursor` được tạo từ hội thoại cuối cùng của page hiện tại, dựa trên cặp giá trị `sortAt` và `conversationId`, sau đó encode thành chuỗi opaque
- Backend phải query page tiếp theo trong cùng snapshot, tức là chỉ lấy các hội thoại có `sortAt <= snapshotAt`
- Nếu trước khi load-more có hội thoại nhận tin nhắn mới và nhảy lên đầu danh sách, hội thoại đó không đi vào page tiếp theo của snapshot cũ; client nhận nó qua realtime event `conversation.updated` hoặc reload page đầu

### 5.8. Chi tiết hội thoại

- Endpoint: `GET /api/v1/conversations/{conversationId}`
- Trả đầy đủ participant và trạng thái hiện tại của hội thoại

Response `200`: dùng cùng schema như `POST /conversations/direct`

### 5.9. Lịch sử tin nhắn

- Endpoint: `GET /api/v1/messages?conversationId=1001&limit=50&cursor=opaque-token`
- Khi không có `cursor`, server trả page gần nhất
- Mỗi page trả `items` theo thứ tự tăng dần của `createdAt` để client render trực tiếp

Response `200`:

```json
{
  "conversationId": 1001,
  "items": [
    {
      "id": 9001,
      "clientMessageId": "c8c4c246-0bb4-4d4f-94e7-512e8d6b6f6b",
      "conversationId": 1001,
      "sender": {
        "id": 1,
        "username": "alice",
        "displayName": "Alice",
        "avatarUrl": null
      },
      "content": "Hello Bob",
      "messageType": "TEXT",
      "status": "READ",
      "createdAt": "2026-05-08T16:02:00Z",
      "deliveredAt": "2026-05-08T16:02:03Z",
      "readAt": "2026-05-08T16:02:05Z"
    }
  ],
  "paging": {
    "limit": 50,
    "nextCursor": "opaque-next-cursor",
    "hasMore": true
  }
}
```

### 5.10. Gửi tin nhắn

- Endpoint: `POST /api/v1/messages`
- `clientMessageId` là bắt buộc để chống duplicate
- Neu day la tin nhan dau tien cua hoi thoai direct, server phai chuyen `isVisibleInList = true` cho participant con lai truoc khi phat event realtime

Request:

```json
{
    "conversationId": 1001,
  "clientMessageId": "c8c4c246-0bb4-4d4f-94e7-512e8d6b6f6b",
  "content": "Hello Bob",
  "messageType": "TEXT"
}
```

Response `201`:

```json
{
  "id": 9001,
  "clientMessageId": "c8c4c246-0bb4-4d4f-94e7-512e8d6b6f6b",
  "conversationId": 1001,
  "sender": {
    "id": 1,
    "username": "alice",
    "displayName": "Alice",
    "avatarUrl": null
  },
  "content": "Hello Bob",
  "messageType": "TEXT",
  "status": "SENT",
  "createdAt": "2026-05-08T16:02:00Z",
  "deliveredAt": null,
  "readAt": null
}
```

Lưu ý:

- Sau khi REST trả `201`, server phải đồng thời phát event realtime `message.created`
- Neu tin nhan dau tien lam hoi thoai direct tu trang thai an sang hien voi nguoi nhan, server phai phat them `conversation.updated` de danh sach chat phia nguoi nhan xuat hien hoi thoai
- Nếu client gửi lại cùng `clientMessageId` trong cùng hội thoại, server nên trả lại message đã tồn tại thay vì tạo bản ghi mới

### 5.11. Đánh dấu đã đọc

- Endpoint: `POST /api/v1/messages/read`
- Dùng để cập nhật mốc read mới nhất của người dùng hiện tại

Request:

```json
{
  "conversationId": 1001,
  "lastReadMessageId": 9001
}
```

Response `200`:

```json
{
  "conversationId": 1001,
  "lastReadMessageId": 9001,
  "readAt": "2026-05-08T16:02:05Z",
  "unreadCount": 0
}
```

Lưu ý:

- Sau khi thành công, server phải phát event realtime `message.read`
- Event này cũng phải kéo theo `conversation.updated` cho cả hai phía nếu số unread thay đổi

## 6. Contract lỗi chung

Format lỗi thống nhất:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-05-08T16:07:10Z",
  "path": "/api/v1/auth/register",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

Mã lỗi đề xuất:

- `VALIDATION_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `NOT_FOUND`
- `CONFLICT`
- `ACCOUNT_INACTIVE`
- `ACCOUNT_LOCKED`
- `MESSAGE_DUPLICATE`

## 7. Contract WebSocket/STOMP

### 7.1. Kết nối

- URL: `ws://{host}/ws`
- Protocol: STOMP 1.2
- Client phải gửi frame `CONNECT` với header:

```text
Authorization: Bearer <accessToken>
accept-version:1.2
heart-beat:10000,10000
```

### 7.2. Subscribe

Client subscribe tối thiểu các destination sau:

- `/user/queue/events`: nhận tất cả event nghiệp vụ dành riêng cho user hiện tại
- `/user/queue/errors`: nhận lỗi realtime nếu server cần push lỗi ở tầng event

### 7.3. Client gửi command

#### Typing

- Destination: `/app/conversations/{conversationId}/typing`

Payload:

```json
{
  "action": "START"
}
```

Hoặc:

```json
{
  "action": "STOP"
}
```

#### Delivered acknowledgement

- Destination: `/app/messages/{messageId}/delivered`

Payload:

```json
{
  "conversationId": 1001,
  "clientReceivedAt": "2026-05-08T16:02:03Z"
}
```

### 7.4. Event envelope

Mọi event push qua `/user/queue/events` dùng envelope chung:

```json
{
  "eventId": "evt_01JTQ49J6ZXW3T1P9F4M6P8M1H",
  "eventType": "message.created",
  "occurredAt": "2026-05-08T16:02:00Z",
  "conversationId": 1001,
  "data": {}
}
```

### 7.5. Các loại event từ server

#### `message.created`

Khi có tin nhắn mới được lưu thành công.

```json
{
  "eventId": "evt_1",
  "eventType": "message.created",
  "occurredAt": "2026-05-08T16:02:00Z",
  "conversationId": 1001,
  "data": {
    "message": {
      "id": 9001,
      "clientMessageId": "c8c4c246-0bb4-4d4f-94e7-512e8d6b6f6b",
      "conversationId": 1001,
      "sender": {
        "id": 1,
        "username": "alice",
        "displayName": "Alice",
        "avatarUrl": null
      },
      "content": "Hello Bob",
      "messageType": "TEXT",
      "status": "SENT",
      "createdAt": "2026-05-08T16:02:00Z",
      "deliveredAt": null,
      "readAt": null
    }
  }
}
```

#### `message.status.updated`

Khi trạng thái tin chuyển sang `DELIVERED` hoặc `READ`.

```json
{
  "eventId": "evt_2",
  "eventType": "message.status.updated",
  "occurredAt": "2026-05-08T16:02:03Z",
  "conversationId": 1001,
  "data": {
    "messageId": 9001,
    "status": "DELIVERED",
    "deliveredAt": "2026-05-08T16:02:03Z",
    "readAt": null
  }
}
```

#### `message.read`

Khi người nhận cập nhật mốc đã đọc.

```json
{
  "eventId": "evt_3",
  "eventType": "message.read",
  "occurredAt": "2026-05-08T16:02:05Z",
  "conversationId": 1001,
  "data": {
    "readerUserId": 2,
    "lastReadMessageId": 9001,
    "readAt": "2026-05-08T16:02:05Z"
  }
}
```

#### `conversation.updated`

Khi summary của hội thoại thay đổi, ví dụ last message, unread count hoặc presence của đối phương trong danh sách chat.

```json
{
  "eventId": "evt_4",
  "eventType": "conversation.updated",
  "occurredAt": "2026-05-08T16:02:05Z",
  "conversationId": 1001,
  "data": {
    "conversation": {
      "id": 1001,
      "type": "DIRECT",
      "otherParticipant": {
        "id": 2,
        "username": "bob",
        "displayName": "Bob",
        "avatarUrl": null,
        "presence": {
          "isOnline": true,
          "lastActiveAt": "2026-05-08T16:02:05Z"
        }
      },
      "unreadCount": 0,
      "lastMessage": {
        "id": 9001,
        "senderId": 1,
        "contentPreview": "Hello Bob",
        "status": "READ",
        "createdAt": "2026-05-08T16:02:00Z"
      },
      "lastMessageAt": "2026-05-08T16:02:00Z",
      "createdAt": "2026-05-08T15:10:00Z",
      "updatedAt": "2026-05-08T16:02:05Z"
    }
  }
}
```

#### `typing.updated`

Khi đối phương bắt đầu hoặc dừng nhập.

```json
{
  "eventId": "evt_5",
  "eventType": "typing.updated",
  "occurredAt": "2026-05-08T16:01:50Z",
  "conversationId": 1001,
  "data": {
    "actorUserId": 2,
    "action": "START",
    "expiresAt": "2026-05-08T16:01:55Z"
  }
}
```

#### `presence.updated`

Khi user online hoặc offline.

```json
{
  "eventId": "evt_6",
  "eventType": "presence.updated",
  "occurredAt": "2026-05-08T16:01:20Z",
  "conversationId": null,
  "data": {
    "userId": 2,
    "isOnline": true,
    "lastActiveAt": "2026-05-08T16:01:20Z"
  }
}
```

## 8. Quy tắc xử lý ở client

- Sau login thành công, client nên:
  1. lưu token
  2. mở kết nối WebSocket/STOMP
  3. subscribe `/user/queue/events`
  4. gọi REST để tải `GET /users/me` và `GET /conversations`
- Khi gửi message qua REST, client có thể hiển thị optimistic bằng `clientMessageId`
- Khi nhận `message.created`, client phải reconcile theo `id` hoặc `clientMessageId`
- Khi nhận `message.status.updated`, client chỉ cập nhật lên trạng thái cao hơn
- Nếu mất kết nối WebSocket, client phải reconnect và sau đó đồng bộ lại bằng REST

## 9. Điểm cố ý chưa đưa vào MVP

- Endpoint logout phía server
- Group chat
- Upload file hoặc media message
- Edit hoặc revoke message
- Push notification ngoài phiên WebSocket hiện tại

## 10. Ghi chú triển khai backend

Contract này bám với bối cảnh codebase hiện tại ở các điểm sau:

- Đăng nhập hỗ trợ `username` hoặc `email`
- Dùng JWT access token và refresh token riêng
- `User` có các field `username`, `email`, `displayName`, `avatarUrl`, `bio`, `accountStatus`
- `accountStatus` hỗ trợ các giá trị `ACTIVE`, `INACTIVE`, `SUSPENDED`, `BANNED`

Nếu backend triển khai khác contract này, cần cập nhật lại file OpenAPI và tài liệu này cùng lúc để tránh lệch giữa backend và frontend.
