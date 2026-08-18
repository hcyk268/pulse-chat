package backend.xxx.chat.message.repository;

import backend.xxx.chat.message.model.MessageAttachment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    @Query("""
        from MessageAttachment attachment
        join fetch attachment.uploadedAsset asset
        join fetch asset.owner
        where attachment.message.id in :messageIds
        order by attachment.message.id asc, attachment.sortOrder asc, attachment.id asc
        """)
    List<MessageAttachment> findByMessageIdInWithUploadedAsset(
            @Param("messageIds") Collection<Long> messageIds
    );

    @Query("""
        from MessageAttachment attachment
        join fetch attachment.uploadedAsset asset
        join fetch asset.owner
        join fetch attachment.message message
        join fetch message.conversation
        where asset.id in :assetIds
        order by message.createdAt desc, attachment.sortOrder asc, attachment.id asc
        """)
    List<MessageAttachment> findByUploadedAssetIdInWithMessageAndConversation(
            @Param("assetIds") Collection<Long> assetIds
    );

}