package backend.xxx.chat.message.model;

import java.util.Objects;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.storage.model.UploadedAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "message_attachments")
@NoArgsConstructor
public class MessageAttachment extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_asset_id", nullable = false, unique = true)
    private UploadedAsset uploadedAsset;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public static MessageAttachment createFromAsset(UploadedAsset asset, int sortOrder) {
        Objects.requireNonNull(asset, "asset must not be null");
        MessageAttachment attachment = new MessageAttachment();
        attachment.uploadedAsset = asset;
        attachment.sortOrder = sortOrder;
        return attachment;
    }

    void attachTo(Message message) {
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    public String getObjectKey() {
        return uploadedAsset.getObjectKey();
    }

    public String getUrl() {
        return uploadedAsset.getPublicUrl();
    }

    public String getFileName() {
        return uploadedAsset.getFileName();
    }

    public String getContentType() {
        return uploadedAsset.getContentType();
    }

    public Long getSizeBytes() {
        return uploadedAsset.getSizeBytes();
    }

    public Integer getWidth() {
        return uploadedAsset.getWidth();
    }

    public Integer getHeight() {
        return uploadedAsset.getHeight();
    }

    public Integer getDurationSeconds() {
        return uploadedAsset.getDurationSeconds();
    }

    public String getThumbnailUrl() {
        return uploadedAsset.getThumbnailUrl();
    }
}
