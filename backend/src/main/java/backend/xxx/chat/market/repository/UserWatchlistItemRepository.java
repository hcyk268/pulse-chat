package backend.xxx.chat.market.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.UserWatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWatchlistItemRepository extends JpaRepository<UserWatchlistItem, Long> {

    @Query("""
            from UserWatchlistItem item
            join item.user user
            join fetch item.asset asset
            where lower(user.username) = lower(:username)
            order by item.createdAt desc, item.id desc
            """)
    List<UserWatchlistItem> findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc(
            @Param("username") String username
    );

    @Query("""
            from UserWatchlistItem item
            join item.user user
            join fetch item.asset asset
            where lower(user.username) = lower(:username)
                and item.id = :id
            """)
    Optional<UserWatchlistItem> findByUser_UsernameIgnoreCaseAndId(
            @Param("username") String username,
            @Param("id") Long id
    );

    Optional<UserWatchlistItem> findByUser_UsernameIgnoreCaseAndAsset_SymbolIgnoreCase(String username, String symbol);

    boolean existsByUser_IdAndAsset_Id(Long userId, Long assetId);
}
