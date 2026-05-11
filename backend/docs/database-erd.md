# Database ERD

Source: current JPA entities under `src/main/java/backend/xxx/chat`.

```mermaid
erDiagram
    USERS {
        BIGINT id PK
        TIMESTAMP created_at
        TIMESTAMP updated_at
        VARCHAR_50 username UK
        VARCHAR_255 email UK
        VARCHAR_255 password_hash
        VARCHAR_100 display_name
        VARCHAR_500 avatar_url
        VARCHAR_500 bio
        VARCHAR_20 account_status
    }

    PRESENCES {
        BIGINT user_id PK,FK
        BOOLEAN is_online
        TIMESTAMP last_active_at
        INT connection_count
    }

    CONVERSATIONS {
        BIGINT id PK
        TIMESTAMP created_at
        TIMESTAMP updated_at
        VARCHAR_20 type
        BIGINT last_message_id
        TIMESTAMP last_message_at
    }

    CONVERSATION_PARTICIPANTS {
        BIGINT conversation_id PK,FK
        BIGINT user_id PK,FK
        TIMESTAMP joined_at
        BIGINT last_read_message_id
        BIGINT unread_count
        BOOLEAN is_visible_in_list
    }

    MESSAGES {
        BIGINT id PK
        TIMESTAMP created_at
        TIMESTAMP updated_at
        BIGINT conversation_id FK
        BIGINT sender_id FK
        VARCHAR_4000 content
        VARCHAR_20 message_type
        VARCHAR_20 status
        TIMESTAMP delivered_at
        TIMESTAMP read_at
    }

    USERS ||--o| PRESENCES : has
    USERS ||--o{ CONVERSATION_PARTICIPANTS : participates
    CONVERSATIONS ||--o{ CONVERSATION_PARTICIPANTS : has
    USERS ||--o{ MESSAGES : sends
    CONVERSATIONS ||--o{ MESSAGES : contains
```

## Notes

- `conversation_participants` uses a composite primary key: `(conversation_id, user_id)`.
- `presences.user_id` is both the primary key and a foreign key to `users.id`.
- `conversations.last_message_id` and `conversation_participants.last_read_message_id` are plain `BIGINT` columns in the current JPA model. They are logical references to `messages.id`, but there is no `@ManyToOne` / `@JoinColumn`, so Hibernate will not create foreign key constraints for them.
- Enum columns are stored as strings:
  - `users.account_status`: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `BANNED`
  - `conversations.type`: `DIRECT`
  - `messages.message_type`: `TEXT`
  - `messages.status`: `SENT`, `DELIVERED`, `READ`
