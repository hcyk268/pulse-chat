package backend.xxx.chat.storage.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.storage.model.UploadPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadPartRepository extends JpaRepository<UploadPart, Long> {

    Optional<UploadPart> findByUploadSessionIdAndPartNumber(Long uploadSessionId, Integer partNumber);

    List<UploadPart> findByUploadSessionIdOrderByPartNumberAsc(Long uploadSessionId);

    long countByUploadSessionId(Long uploadSessionId);

    @Query("""
            select part.partNumber
            from UploadPart part
            where part.uploadSession.id = :uploadSessionId
            order by part.partNumber asc
            """)
    List<Integer> findUploadedPartNumbers(@Param("uploadSessionId") Long uploadSessionId);
}