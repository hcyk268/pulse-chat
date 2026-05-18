package backend.xxx.chat.user.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@Table(name = "presences")
@NoArgsConstructor
@AllArgsConstructor
public class Presence implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "connection_count", nullable = false)
    private int connectionCount;

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public static Presence offline(User user) {
        Presence presence = new Presence();
        presence.user = user;
        presence.userId = user.getId();
        presence.online = false;
        presence.connectionCount = 0;
        return presence;
    }

    public void markOnline(Instant lastActiveAt) {
        this.online = true;
        this.lastActiveAt = lastActiveAt;
        this.connectionCount = Math.max(1, this.connectionCount + 1);
    }

    public void markOffline(Instant lastActiveAt) {
        this.connectionCount = Math.max(0, this.connectionCount - 1);
        this.online = this.connectionCount > 0;
        this.lastActiveAt = lastActiveAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
