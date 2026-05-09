package backend.xxx.chat.user.repository;

import java.util.Collection;
import java.util.List;

import backend.xxx.chat.user.model.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    List<Presence> findByUserIdIn(Collection<Long> userIds);
}
