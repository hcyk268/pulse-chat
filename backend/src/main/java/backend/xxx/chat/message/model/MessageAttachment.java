package backend.xxx.chat.message.model;

import java.util.Objects;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.storage.model.UploadedAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "message_attachments")
@NoArgsConstructor
public class MessageAttachment extends AbstractBaseEntity<Long> {

    private static final int OBJECT_KEY_MAX_LENGTH = 1000;
    private static final int URL_MAX_LENGTH = 2000;
    private static final int FILE_NAME_MAX_LENGTH = 255;
    private static final int CONTENT_TYPE_MAX_LENGTH = 100;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_asset_id")
    private UploadedAsset uploadedAsset;

    @Column(name = "object_key", nullable = false, length = OBJECT_KEY_MAX_LENGTH)
    private String objectKey;

    @Column(name = "url", length = URL_MAX_LENGTH)
    private String url;

    @Column(name = "file_name", nullable = false, length = FILE_NAME_MAX_LENGTH)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = CONTENT_TYPE_MAX_LENGTH)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = URL_MAX_LENGTH)
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public static MessageAttachment create(
            String objectKey,
            String url,
            String fileName,
            String contentType,
            Long sizeBytes,
            Integer width,
            Integer height,
            Integer durationSeconds,
            String thumbnailUrl,
            int sortOrder
    ) {
        MessageAttachment attachment = new MessageAttachment();
        attachment.objectKey = requireTrimmed(objectKey, "objectKey must not be blank");
        attachment.url = normalizeOptional(url);
        attachment.fileName = requireTrimmed(fileName, "fileName must not be blank");
        attachment.contentType = requireTrimmed(contentType, "contentType must not be blank");
        attachment.sizeBytes = requirePositive(sizeBytes, "sizeBytes must be greater than 0");
        attachment.width = normalizePositive(width, "width must be greater than 0");
        attachment.height = normalizePositive(height, "height must be greater than 0");
        attachment.durationSeconds = normalizePositive(durationSeconds, "durationSeconds must be greater than 0");
        attachment.thumbnailUrl = normalizeOptional(thumbnailUrl);
        attachment.sortOrder = sortOrder;
        return attachment;
    }

    public static MessageAttachment createFromAsset(UploadedAsset asset, int sortOrder) {
        Objects.requireNonNull(asset, "asset must not be null");
        MessageAttachment attachment = create(
                asset.getObjectKey(),
                asset.getPublicUrl(),
                asset.getFileName(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getDurationSeconds(),
                asset.getThumbnailUrl(),
                sortOrder
        );
        attachment.uploadedAsset = asset;
        return attachment;
    }

    void attachTo(Message message) {
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    private static String requireTrimmed(String value, String message) {
        Objects.requireNonNull(value, message);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Long requirePositive(Long value, String message) {
        Objects.requireNonNull(value, message);
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer normalizePositive(Integer value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}