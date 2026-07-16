DO $$
DECLARE
    legacy_attachment RECORD;
    new_upload_session_id BIGINT;
    new_uploaded_asset_id BIGINT;
BEGIN
    FOR legacy_attachment IN
        SELECT
            attachment.id,
            attachment.created_at,
            attachment.updated_at,
            attachment.object_key,
            attachment.url,
            attachment.file_name,
            attachment.content_type,
            attachment.size_bytes,
            attachment.width,
            attachment.height,
            attachment.duration_seconds,
            attachment.thumbnail_url,
            message.sender_id AS owner_id
        FROM message_attachments attachment
        JOIN messages message ON message.id = attachment.message_id
        WHERE attachment.uploaded_asset_id IS NULL
        ORDER BY attachment.id
    LOOP
        INSERT INTO upload_sessions (
            created_at,
            updated_at,
            owner_id,
            purpose,
            file_name,
            content_type,
            size_bytes,
            chunk_size_bytes,
            total_parts,
            object_key,
            r2_upload_id,
            file_checksum,
            status,
            expires_at,
            completed_at
        ) VALUES (
            legacy_attachment.created_at,
            legacy_attachment.updated_at,
            legacy_attachment.owner_id,
            'MESSAGE_ATTACHMENT',
            legacy_attachment.file_name,
            legacy_attachment.content_type,
            legacy_attachment.size_bytes,
            legacy_attachment.size_bytes,
            1,
            legacy_attachment.object_key,
            'legacy-message-attachment-' || legacy_attachment.id,
            NULL,
            'ATTACHED',
            legacy_attachment.created_at + INTERVAL '100 years',
            legacy_attachment.created_at
        ) RETURNING id INTO new_upload_session_id;

        INSERT INTO uploaded_assets (
            created_at,
            updated_at,
            owner_id,
            upload_session_id,
            purpose,
            object_key,
            public_url,
            file_name,
            content_type,
            size_bytes,
            width,
            height,
            duration_seconds,
            thumbnail_url,
            status,
            attached_at
        ) VALUES (
            legacy_attachment.created_at,
            legacy_attachment.updated_at,
            legacy_attachment.owner_id,
            new_upload_session_id,
            'MESSAGE_ATTACHMENT',
            legacy_attachment.object_key,
            legacy_attachment.url,
            legacy_attachment.file_name,
            legacy_attachment.content_type,
            legacy_attachment.size_bytes,
            legacy_attachment.width,
            legacy_attachment.height,
            legacy_attachment.duration_seconds,
            legacy_attachment.thumbnail_url,
            'ATTACHED',
            legacy_attachment.created_at
        ) RETURNING id INTO new_uploaded_asset_id;

        UPDATE message_attachments
        SET uploaded_asset_id = new_uploaded_asset_id
        WHERE id = legacy_attachment.id;
    END LOOP;
END $$;

ALTER TABLE message_attachments
    ALTER COLUMN uploaded_asset_id SET NOT NULL;

ALTER TABLE message_attachments
    ADD CONSTRAINT uk_message_attachments_uploaded_asset UNIQUE (uploaded_asset_id);

ALTER TABLE message_attachments
    DROP COLUMN object_key,
    DROP COLUMN url,
    DROP COLUMN file_name,
    DROP COLUMN content_type,
    DROP COLUMN size_bytes,
    DROP COLUMN width,
    DROP COLUMN height,
    DROP COLUMN duration_seconds,
    DROP COLUMN thumbnail_url;
