package backend.xxx.chat.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.notification.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "actor")
    @Query("""
            from Notification notification
            where lower(notification.recipient.username) = lower(:username)
                and (:beforeId is null or notification.id < :beforeId)
            order by notification.id desc
            """)
    List<Notification> findPageByRecipientUsername(
            @Param("username") String username,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "actor")
    Optional<Notification> findByRecipient_UsernameIgnoreCaseAndId(String username, Long id);

    long countByRecipient_UsernameIgnoreCaseAndReadAtIsNull(String username);

    boolean existsByRecipient_IdAndDedupeKey(Long recipientId, String dedupeKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification notification
            set notification.readAt = :readAt
            where notification.recipient.id = :recipientId
                and notification.readAt is null
            """)
    int markAllReadByRecipientId(
            @Param("recipientId") Long recipientId,
            @Param("readAt") Instant readAt
    );
}
