package backend.xxx.chat.storage.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.storage.model.UploadedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UploadedAssetRepository extends JpaRepository<UploadedAsset, Long> {

    Optional<UploadedAsset> findByUploadSessionId(Long uploadSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            from UploadedAsset asset
            join fetch asset.owner
            join fetch asset.uploadSession
            where asset.id = :assetId
            """)
    Optional<UploadedAsset> findByIdForUpdate(@Param("assetId") Long assetId);

    @Query("""
            from UploadedAsset asset
            join fetch asset.owner
            join fetch asset.uploadSession
            where asset.id in :assetIds
            """)
    List<UploadedAsset> findByIdInWithOwnerAndSession(@Param("assetIds") Collection<Long> assetIds);
}