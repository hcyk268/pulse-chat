package backend.xxx.chat.user.repository;

import java.time.Instant;

import backend.xxx.chat.config.JpaAuditingConfig;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PresenceRepositoryTest {

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savePersistsNewPresenceWithMapsIdInsteadOfMergingDetachedEntity() {
        User user = User.create("alice", "alice@example.com", "hashed-password", "Alice");
        entityManager.persist(user);
        entityManager.flush();

        Presence presence = Presence.offline(user);
        presence.markOnline(Instant.parse("2026-01-01T00:00:00Z"));

        presenceRepository.saveAndFlush(presence);
        entityManager.clear();

        Presence savedPresence = presenceRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(savedPresence.getUserId()).isEqualTo(user.getId());
        assertThat(savedPresence.isOnline()).isTrue();
        assertThat(savedPresence.getConnectionCount()).isEqualTo(1);
        assertThat(savedPresence.isNew()).isFalse();
    }
}
