package backend.xxx.chat.storage.model;

import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "upload_parts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_upload_parts_session_part",
                columnNames = {"upload_session_id", "part_number"}
        )
)
@NoArgsConstructor
public class UploadPart extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_session_id", nullable = false)
    private UploadSession uploadSession;

    @Column(name = "part_number", nullable = false)
    private Integer partNumber;

    @Column(name = "etag", nullable = false, length = 500)
    private String etag;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public static UploadPart create(
            UploadSession uploadSession,
            Integer partNumber,
            String etag,
            Long sizeBytes,
            Instant uploadedAt
    ) {
        UploadPart part = new UploadPart();
        part.uploadSession = uploadSession;
        part.partNumber = partNumber;
        part.etag = etag;
        part.sizeBytes = sizeBytes;
        part.uploadedAt = uploadedAt;
        return part;
    }

    public void update(String etag, Long sizeBytes, Instant uploadedAt) {
        this.etag = etag;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }
}