package backend.xxx.chat.message.repository;

import backend.xxx.chat.message.model.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    @Query("""
        from MessageAttachment attachment
        join fetch attachment.uploadedAsset
        where attachment.message.id in :messageIds
        order by attachment.message.id asc, attachment.sortOrder asc, attachment.id asc
        """)
    List<MessageAttachment> findByMessageIdInWithUploadedAsset(
            @Param("messageIds") Collection<Long> messageIds
    );
}
