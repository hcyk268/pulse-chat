package backend.xxx.chat.market.service;

import java.util.List;
import java.util.Locale;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.UserNotFoundException;
import backend.xxx.chat.market.dto.WatchlistItemRequest;
import backend.xxx.chat.market.dto.WatchlistItemResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.UserWatchlistItem;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.UserWatchlistItemRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final UserWatchlistItemRepository userWatchlistItemRepository;
    private final MarketAssetRepository marketAssetRepository;
    private final UserRepository userRepository;
    private final MarketMapper marketMapper;

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> getWatchlist(String username) {
        return userWatchlistItemRepository.findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc(username)
                .stream()
                .filter(UserWatchlistItem::hasActiveAsset)
                .map(marketMapper::toWatchlistItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WatchlistItemResponse getWatchlistItem(String username, Long itemId) {
        return marketMapper.toWatchlistItemResponse(findActiveUserItem(username, itemId));
    }

    @Transactional
    public WatchlistItemResponse addWatchlistItem(String username, WatchlistItemRequest request) {
        User user = resolveUser(username);
        MarketAsset asset = resolveAsset(request.symbol());
        userWatchlistItemRepository.findByUser_UsernameIgnoreCaseAndAsset_SymbolIgnoreCase(username, asset.getSymbol())
                .ifPresent(item -> {
                    throw new ConflictException("market.watchlist.item.already.exists");
                });

        UserWatchlistItem item = UserWatchlistItem.create(user, asset);
        return marketMapper.toWatchlistItemResponse(userWatchlistItemRepository.save(item));
    }

    @Transactional
    public WatchlistItemResponse updateWatchlistItem(String username, Long itemId, WatchlistItemRequest request) {
        UserWatchlistItem item = findActiveUserItem(username, itemId);
        MarketAsset asset = resolveAsset(request.symbol());
        if (item.getAsset().getId().equals(asset.getId())) {
            return marketMapper.toWatchlistItemResponse(item);
        }

        if (userWatchlistItemRepository.existsByUser_IdAndAsset_Id(item.getUser().getId(), asset.getId())) {
            throw new ConflictException("market.watchlist.item.already.exists");
        }

        item.changeAsset(asset);
        return marketMapper.toWatchlistItemResponse(userWatchlistItemRepository.save(item));
    }

    @Transactional
    public void removeWatchlistItem(String username, Long itemId) {
        userWatchlistItemRepository.delete(findUserItem(username, itemId));
    }

    @Transactional
    public void removeWatchlistItemBySymbol(String username, String symbol) {
        UserWatchlistItem item = userWatchlistItemRepository
                .findByUser_UsernameIgnoreCaseAndAsset_SymbolIgnoreCase(username, normalizeSymbol(symbol))
                .orElseThrow(() -> new NotFoundException("market.watchlist.item.not.found"));
        userWatchlistItemRepository.delete(item);
    }

    private UserWatchlistItem findUserItem(String username, Long itemId) {
        return userWatchlistItemRepository.findByUser_UsernameIgnoreCaseAndId(username, itemId)
                .orElseThrow(() -> new NotFoundException("market.watchlist.item.not.found"));
    }

    private UserWatchlistItem findActiveUserItem(String username, Long itemId) {
        UserWatchlistItem item = findUserItem(username, itemId);
        if (!item.hasActiveAsset()) {
            throw new NotFoundException("market.watchlist.item.not.found");
        }
        return item;
    }

    private User resolveUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(UserNotFoundException::new);
    }

    private MarketAsset resolveAsset(String symbol) {
        return marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue(normalizeSymbol(symbol))
                .orElseThrow(() -> new NotFoundException("market.coin.not.found"));
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
