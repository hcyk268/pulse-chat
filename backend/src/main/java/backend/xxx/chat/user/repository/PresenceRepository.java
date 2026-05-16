package backend.xxx.chat.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.user.model.Presence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    List<Presence> findByUserIdIn(Collection<Long> userIds);

    Optional<Presence> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            from Presence presence
            join fetch presence.user
            where presence.userId = :userId
            """)
    Optional<Presence> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("""
            update Presence presence
            set presence.online = false,
                presence.connectionCount = 0
            where presence.online = true
                or presence.connectionCount <> 0
            """)
    int resetAllConnections();
}
