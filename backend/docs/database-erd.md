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
        UUID client_message_id
        BIGINT conversation_id FK
        BIGINT sender_id FK
        VARCHAR_4000 content
        BIGINT reply_to_message_id FK
        VARCHAR_20 message_type
        VARCHAR_20 status
        TIMESTAMP delivered_at
        TIMESTAMP read_at
        TIMESTAMP edited_at
        BIGINT deleted_by FK
        TIMESTAMP deleted_at
    }

    MESSAGE_PINS {
        BIGINT id PK
        TIMESTAMP created_at
        TIMESTAMP updated_at
        BIGINT conversation_id FK
        BIGINT message_id FK
        BIGINT pinned_by FK
        TIMESTAMP pinned_at
    }

    MESSAGE_REACTIONS {
        BIGINT id PK
        TIMESTAMP created_at
        TIMESTAMP updated_at
        BIGINT message_id FK
        BIGINT user_id FK
        VARCHAR_32 emoji
    }

    USERS ||--o| PRESENCES : has
    USERS ||--o{ CONVERSATION_PARTICIPANTS : participates
    CONVERSATIONS ||--o{ CONVERSATION_PARTICIPANTS : has
    USERS ||--o{ MESSAGES : sends
    USERS ||--o{ MESSAGES : deletes
    CONVERSATIONS ||--o{ MESSAGES : contains
    MESSAGES ||--o{ MESSAGES : replies_to
    CONVERSATIONS ||--o{ MESSAGE_PINS : has
    MESSAGES ||--o{ MESSAGE_PINS : pinned_as
    USERS ||--o{ MESSAGE_PINS : pins
    MESSAGES ||--o{ MESSAGE_REACTIONS : receives
    USERS ||--o{ MESSAGE_REACTIONS : reacts
```

## Notes

- `conversation_participants` uses a composite primary key: `(conversation_id, user_id)`.
- `presences.user_id` is both the primary key and a foreign key to `users.id`.
- `messages` has a unique constraint on `(conversation_id, client_message_id)` to make client-side retry idempotency safe at the database layer.
- `conversations.last_message_id` and `conversation_participants.last_read_message_id` are plain `BIGINT` columns in the current JPA model. They are logical references to `messages.id`, but there is no `@ManyToOne` / `@JoinColumn`, so Hibernate will not create foreign key constraints for them.
- `messages.reply_to_message_id` is a nullable self-reference used for reply messages.
- Soft-deleted/unsent messages keep their row and original content in storage, but application responses should hide `content` when `deleted_at` is set.
- `message_pins` is separate from `messages` so pin metadata can track who pinned and when. The max pinned-message count per conversation should be enforced in application service logic.
- `message_reactions` uses `UNIQUE(message_id, user_id, emoji)` so one user cannot add the same emoji to the same message more than once.
- Enum columns are stored as strings:
  - `users.account_status`: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `BANNED`
  - `conversations.type`: `DIRECT`
  - `messages.message_type`: `TEXT`
  - `messages.status`: `SENT`, `DELIVERED`, `READ`
  - `message_reactions.emoji`: `LIKE`, `LOVE`, `HAHA`, `WOW`, `SAD`, `ANGRY`
