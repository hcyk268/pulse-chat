package backend.xxx.chat.market.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.UserWatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWatchlistItemRepository extends JpaRepository<UserWatchlistItem, Long> {

    List<UserWatchlistItem> findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc(String username);

    Optional<UserWatchlistItem> findByUser_UsernameIgnoreCaseAndId(String username, Long id);

    Optional<UserWatchlistItem> findByUser_UsernameIgnoreCaseAndAsset_SymbolIgnoreCase(String username, String symbol);

    boolean existsByUser_IdAndAsset_Id(Long userId, Long assetId);
}