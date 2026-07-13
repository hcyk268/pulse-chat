package backend.xxx.chat.storage.model;

import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "uploaded_assets")
@NoArgsConstructor
public class UploadedAsset extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_session_id", nullable = false)
    private UploadSession uploadSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private UploadPurpose purpose;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "public_url", length = 2000)
    private String publicUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 2000)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UploadedAssetStatus status = UploadedAssetStatus.READY;

    @Column(name = "attached_at")
    private Instant attachedAt;

    public static UploadedAsset readyFromSession(UploadSession session, String publicUrl, Long verifiedSizeBytes) {
        UploadedAsset asset = new UploadedAsset();
        asset.owner = session.getOwner();
        asset.uploadSession = session;
        asset.purpose = session.getPurpose();
        asset.objectKey = session.getObjectKey();
        asset.publicUrl = publicUrl;
        asset.fileName = session.getFileName();
        asset.contentType = session.getContentType();
        asset.sizeBytes = verifiedSizeBytes == null ? session.getSizeBytes() : verifiedSizeBytes;
        asset.status = UploadedAssetStatus.READY;
        return asset;
    }

    public boolean belongsTo(Long userId) {
        return owner != null && owner.getId() != null && owner.getId().equals(userId);
    }

    public boolean isReady() {
        return status == UploadedAssetStatus.READY;
    }

    public void markAttached(Instant attachedAt) {
        this.status = UploadedAssetStatus.ATTACHED;
        this.attachedAt = attachedAt;
        this.uploadSession.markAttached();
    }
}