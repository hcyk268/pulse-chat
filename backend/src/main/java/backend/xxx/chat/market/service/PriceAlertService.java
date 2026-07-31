package backend.xxx.chat.market.service;

import java.util.List;
import java.util.Locale;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.UserNotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.market.dto.CreatePriceAlertRequest;
import backend.xxx.chat.market.dto.PriceAlertResponse;
import backend.xxx.chat.market.dto.UpdatePriceAlertRequest;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.PriceAlert;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.PriceAlertRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private static final String BINANCE_EXCHANGE = "BINANCE";

    private final PriceAlertRepository priceAlertRepository;
    private final MarketAssetRepository marketAssetRepository;
    private final MarketPairRepository marketPairRepository;
    private final UserRepository userRepository;
    private final MarketMapper marketMapper;
    private final PriceAlertRegistry priceAlertRegistry;

    @Transactional(readOnly = true)
    public List<PriceAlertResponse> getPriceAlerts(String username) {
        return priceAlertRepository.findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc(username)
                .stream()
                .map(marketMapper::toPriceAlertResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PriceAlertResponse getPriceAlert(String username, Long alertId) {
        return marketMapper.toPriceAlertResponse(findUserAlert(username, alertId));
    }

    @Transactional
    public PriceAlertResponse createPriceAlert(String username, CreatePriceAlertRequest request) {
        User user = resolveUser(username);
        MarketAsset asset = resolveAsset(request.symbol());
        MarketPair pair = resolveBinancePair(asset);

        PriceAlert alert = PriceAlert.create(
                user,
                asset,
                pair,
                request.conditionType(),
                request.targetPrice(),
                request.targetPercent(),
                request.active() == null || request.active()
        );

        PriceAlert savedAlert = priceAlertRepository.save(alert);
        priceAlertRegistry.refreshPairsAfterCommit(List.of(pair.getId()));
        return marketMapper.toPriceAlertResponse(savedAlert);
    }

    @Transactional
    public PriceAlertResponse updatePriceAlert(String username, Long alertId, UpdatePriceAlertRequest request) {
        if (isEmptyUpdate(request)) {
            throw new ValidationException("market.price-alert.update.empty");
        }

        PriceAlert alert = findUserAlert(username, alertId);
        Long previousPairId = alert.getPair().getId();

        if (request.symbol() != null) {
            MarketAsset asset = resolveAsset(request.symbol());
            alert.changeMarket(asset, resolveBinancePair(asset));
        }

        if (request.conditionType() != null || request.targetPrice() != null || request.targetPercent() != null) {
            alert.patchRule(request.conditionType(), request.targetPrice(), request.targetPercent());
        }

        if (request.active() != null) {
            alert.changeActive(request.active());
        }

        PriceAlert savedAlert = priceAlertRepository.save(alert);
        priceAlertRegistry.refreshPairsAfterCommit(List.of(previousPairId, savedAlert.getPair().getId()));
        return marketMapper.toPriceAlertResponse(savedAlert);
    }

    @Transactional
    public void deletePriceAlert(String username, Long alertId) {
        PriceAlert alert = findUserAlert(username, alertId);
        Long pairId = alert.getPair().getId();
        priceAlertRepository.delete(alert);
        priceAlertRegistry.refreshPairsAfterCommit(List.of(pairId));
    }

    private PriceAlert findUserAlert(String username, Long alertId) {
        return priceAlertRepository.findByUser_UsernameIgnoreCaseAndId(username, alertId)
                .orElseThrow(() -> new NotFoundException("market.price-alert.item.not.found"));
    }

    private User resolveUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(UserNotFoundException::new);
    }

    private MarketAsset resolveAsset(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol.isBlank()) {
            throw new ValidationException("market.price-alert.symbol.blank");
        }
        return marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue(normalizedSymbol)
                .orElseThrow(() -> new NotFoundException("market.coin.not.found"));
    }

    private MarketPair resolveBinancePair(MarketAsset asset) {
        return marketPairRepository.findFirstByAsset_IdAndExchangeAndActiveTrue(asset.getId(), BINANCE_EXCHANGE)
                .orElseThrow(() -> new NotFoundException("market.pair.not.found"));
    }

    private boolean isEmptyUpdate(UpdatePriceAlertRequest request) {
        return request.symbol() == null
                && request.conditionType() == null
                && request.targetPrice() == null
                && request.targetPercent() == null
                && request.active() == null;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
