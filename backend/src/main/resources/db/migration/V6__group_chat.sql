ALTER TABLE conversations
    ADD COLUMN name VARCHAR(100),
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN created_by BIGINT;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_created_by
        FOREIGN KEY (created_by) REFERENCES users (id);


ALTER TABLE conversation_participants
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN added_by BIGINT,
    ADD COLUMN left_at TIMESTAMP WITH TIME ZONE;


ALTER TABLE conversation_participants
    ADD CONSTRAINT fk_participants_added_by
        FOREIGN KEY (added_by) REFERENCES users (id);

ALTER TABLE conversation_participants
    ADD CONSTRAINT fk_participants_last_read_message
        FOREIGN KEY (last_read_message_id) REFERENCES messages (id);


CREATE TABLE message_reads (
   message_id BIGINT NOT NULL,
   user_id BIGINT NOT NULL,
   read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   CONSTRAINT pk_message_reads PRIMARY KEY (message_id, user_id),
   CONSTRAINT fk_message_reads_message FOREIGN KEY (message_id) REFERENCES messages (id),
   CONSTRAINT fk_message_reads_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_message_reads_user
    ON message_reads (user_id, message_id);