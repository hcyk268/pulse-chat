package backend.xxx.chat.user.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.user.model.AccountStatus;
import backend.xxx.chat.user.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            from User user
            where user.accountStatus = :activeStatus
                and user.id <> :currentUserId
                and (
                    lower(user.username) like lower(concat('%', :keyword, '%'))
                    or lower(user.displayName) like lower(concat('%', :keyword, '%'))
                    or lower(user.email) like lower(concat('%', :keyword, '%'))
                )
            order by lower(user.displayName), lower(user.username), user.id
            """)
    List<User> searchActiveUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("keyword") String keyword,
            @Param("activeStatus") AccountStatus activeStatus,
            Pageable pageable
    );
}
