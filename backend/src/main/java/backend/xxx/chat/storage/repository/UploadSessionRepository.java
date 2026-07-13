package backend.xxx.chat.storage.repository;

import java.util.Optional;

import backend.xxx.chat.storage.model.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UploadSessionRepository extends JpaRepository<UploadSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            from UploadSession session
            join fetch session.owner
            where session.id = :sessionId
            """)
    Optional<UploadSession> findByIdForUpdate(@Param("sessionId") Long sessionId);

    @Query("""
            from UploadSession session
            join fetch session.owner
            where session.id = :sessionId
            """)
    Optional<UploadSession> findByIdWithOwner(@Param("sessionId") Long sessionId);
}