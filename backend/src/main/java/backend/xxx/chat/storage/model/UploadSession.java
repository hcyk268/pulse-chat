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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "upload_sessions")
@NoArgsConstructor
public class UploadSession extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private UploadPurpose purpose;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "chunk_size_bytes", nullable = false)
    private Long chunkSizeBytes;

    @Column(name = "total_parts", nullable = false)
    private Integer totalParts;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "r2_upload_id", nullable = false, length = 1000)
    private String r2UploadId;

    @Column(name = "file_checksum", length = 128)
    private String fileChecksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UploadSessionStatus status = UploadSessionStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static UploadSession create(
            User owner,
            UploadPurpose purpose,
            String fileName,
            String contentType,
            Long sizeBytes,
            Long chunkSizeBytes,
            Integer totalParts,
            String objectKey,
            String r2UploadId,
            String fileChecksum,
            Instant expiresAt
    ) {
        UploadSession session = new UploadSession();
        session.owner = owner;
        session.purpose = purpose;
        session.fileName = fileName;
        session.contentType = contentType;
        session.sizeBytes = sizeBytes;
        session.chunkSizeBytes = chunkSizeBytes;
        session.totalParts = totalParts;
        session.objectKey = objectKey;
        session.r2UploadId = r2UploadId;
        session.fileChecksum = fileChecksum;
        session.expiresAt = expiresAt;
        session.status = UploadSessionStatus.PENDING;
        return session;
    }

    public boolean belongsTo(Long userId) {
        return owner != null && owner.getId() != null && owner.getId().equals(userId);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public void markUploading() {
        if (status == UploadSessionStatus.PENDING) {
            status = UploadSessionStatus.UPLOADING;
        }
    }

    public void markCompleted(Instant completedAt) {
        this.status = UploadSessionStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void markVerified() {
        this.status = UploadSessionStatus.VERIFIED;
    }

    public void markAttached() {
        this.status = UploadSessionStatus.ATTACHED;
    }

    public void markCancelled() {
        this.status = UploadSessionStatus.CANCELLED;
    }

    public void markFailed() {
        this.status = UploadSessionStatus.FAILED;
    }
}